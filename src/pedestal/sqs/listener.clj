(ns pedestal.sqs.listener
  (:require [clojure.spec.alpha :as s]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.interceptor.chain :as interceptor.chain]
            [io.pedestal.log :as log]
            [io.pedestal.http :as bootstrap]
            [pedestal.sqs.queue :as queue]
            [pedestal.sqs.messaging :as messaging]
            [pedestal.sqs.interceptors :as sqs.interceptors]
            [pedestal.sqs :as sqs]))

;; Utility listener

(defn- clean-service-map-to-queue-fn
  [service-map]
  (-> service-map
      (#(update-in % [:queue] dissoc :queue-fn))
      (dissoc :sqs-client)))

(defn- sqs-deletion-policy-always
  [service-map]
  (let [{:keys [sqs-client queue message]} service-map
        {:keys [queue-fn queue-id]} queue]

    (messaging/delete-message sqs-client queue-id (:ReceiptHandle message))
    (queue-fn (clean-service-map-to-queue-fn service-map))))

(defn- sqs-deletion-policy-on-success
  [service-map]
  (let [{:keys [sqs-client queue message]} service-map
        {:keys [queue-fn queue-id]} queue]

    (queue-fn (clean-service-map-to-queue-fn service-map))
    (messaging/delete-message sqs-client queue-id (:ReceiptHandle message))))

(defn- sqs-handle-deletion-policy
  [service-map]
  (condp = (:deletion-policy service-map)
    :always (sqs-deletion-policy-always service-map)
    :on-success (sqs-deletion-policy-on-success service-map)
    ((:queue-fn (:queue service-map)) (clean-service-map-to-queue-fn service-map))))

(defn- sqs-handle-response-type
  [type]
  (condp = type
    :json sqs.interceptors/json-parser
    sqs.interceptors/default-parser))

(defn- sqs-start-listener
  [service-map]
  (let [{:keys [sqs-client queue]} service-map
        {:keys [queue-id queue-configurations]} queue

        queue-response (messaging/receive-message sqs-client queue-id queue-configurations)
        queue-response-type (sqs-handle-response-type (::sqs/response-type queue-configurations))

        interceptors (or (::sqs/response-interceptors queue-configurations) [queue-response-type])]

    (if queue-response
      ;; reference in https://github.com/cognitect-labs/pedestal.kafka/blob/master/src/com/cognitect/kafka/consumer.clj#L120
      (sqs-handle-deletion-policy
        (interceptor.chain/execute
          (-> service-map
              (assoc :message (first queue-response)
                     :deletion-policy (::sqs/deletion-policy queue-configurations)))
          interceptors))
      nil)))

(defn- stopper
  [service-map]
  (let [continue? (:continue? service-map)]
    (reset! continue? false)
    (doseq [async-listener (:async-listeners service-map)]
      (future-cancel async-listener))
    service-map))

(defn- starter
  [service-map]
  ;; reference in https://groups.google.com/d/msg/clojure/H9tk04sSTWE/5NF6rAG3CwAJ
  {:pre [(or (s/valid? ::sqs/service-map-in service-map)
             (s/explain ::sqs/service-map-in service-map))]}
  (let [{::sqs/keys [client listeners]} (s/conform ::sqs/service-map-in service-map)

        sqs-client (queue/create-sqs-client client)

        queue-configuration (::sqs/configurations service-map {})

        ;; inspired by https://github.com/cognitect-labs/pedestal.kafka/blob/master/src/com/cognitect/kafka/consumer.clj#L139
        continue? (atom true)

        service-map (-> service-map
                        (assoc :sqs-client sqs-client)
                        (assoc ::sqs-stop-fn stopper)
                        (assoc :continue? continue?)
                        (dissoc ::sqs-start-fn))

        listeners (for [listener listeners]
                    (let [queue-name (:queue-name listener)

                          exist-queue-id (queue/get-queue-id sqs-client queue-name)

                          queue-id (if (and (:auto-create-queue? queue-configuration) (not exist-queue-id))
                                     (queue/create-queue sqs-client queue-name)
                                     exist-queue-id)]

                      (assoc listener :queue-id queue-id)))

        ;; reference in https://github.com/cognitect-labs/pedestal.kafka/blob/master/src/com/cognitect/kafka.clj#L43
        ;; other reference in https://github.com/spring-cloud/spring-cloud-aws/blob/v2.0.0.M4/spring-cloud-aws-messaging/src/main/java/org/springframework/cloud/aws/messaging/listener/SimpleMessageListenerContainer.java#L279
        async-listeners (for [listener listeners]
                          (let [queue-name (:queue-name listener)]
                            (log/info :sqs (str "SQS queue register '" queue-name "'"))
                            (future
                              (while @continue?
                                (sqs-start-listener (assoc service-map :queue listener))))))

        service-map (assoc service-map :async-listeners async-listeners)]

    (log/info :sqs "Starting listener SQS queues")

    (let [bootstrapped-service-map (if (::bootstrap/routes service-map) (bootstrap/default-interceptors service-map) service-map)
          default-interceptors (::bootstrap/interceptors bootstrapped-service-map)
          interceptor-with-sqs {:name  ::sqs-components
                                :enter (fn [context]
                                         (-> context
                                             (assoc-in [:request :sqs-client] sqs-client)
                                             (assoc-in
                                               [:request :queues]
                                               (into {} (map #(hash-map (:queue-name %) (:queue-id %)) listeners)))))}]

      (assoc
        bootstrapped-service-map
        ::bootstrap/interceptors
        (into default-interceptors [(interceptor/interceptor interceptor-with-sqs)])))))

;; Core functions

(def start (sqs/service-fn ::sqs-start-fn))

(def stop (sqs/service-fn ::sqs-stop-fn))

(defn sqs-server
  [service-map]
  (assoc service-map ::sqs-start-fn starter))
