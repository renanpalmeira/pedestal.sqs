(ns pedestal.sqs.listener
  (:require [clojure.core.async :as a]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.interceptor.chain :as interceptor.chain]
            [io.pedestal.log :as log]
            [io.pedestal.http :as bootstrap]
            [pedestal.sqs.queue :as queue]
            [pedestal.sqs.messaging :as messaging]
            [pedestal.sqs.interceptors :as sqs.interceptors]
            [pedestal.sqs :as sqs]))

;; Utility listener

(defn clean-service-map-to-queue-fn
  [service-map]
  (dissoc service-map [:queue-fn :sqs-client]))

(defn sqs-deletion-policy-always
  [service-map]
  (let [{:keys [sqs-client
                queue-fn
                queue-id
                message]} service-map]

    (messaging/delete-message sqs-client queue-id (:ReceiptHandle message))
    (queue-fn (clean-service-map-to-queue-fn service-map))))

(defn sqs-deletion-policy-on-success
  [service-map]
  (let [{:keys [sqs-client
                queue-fn
                queue-id
                message]} service-map]

    (queue-fn (clean-service-map-to-queue-fn service-map))
    (messaging/delete-message sqs-client queue-id (:ReceiptHandle message))))

(defn sqs-handle-deletion-policy
  [service-map]
  (condp = (:deletion-policy service-map)
    :always (sqs-deletion-policy-always service-map)
    :on-success (sqs-deletion-policy-on-success service-map)
    ((:queue-fn service-map) (clean-service-map-to-queue-fn service-map))))

(defn sqs-handle-response-type
  [type]
  (condp = type
    :json sqs.interceptors/json-parser
    sqs.interceptors/default-parser))

;; TODO: remove from here check exist queue or not
(defn sqs-start-listener
  [sqs-client listener service-map]
  (let [queue-configuration (::sqs/configurations service-map {})

        queue-name (listener 0)
        queue-fn (listener 1)
        listener-configuration (get listener 2 {})

        exist-queue-id (queue/get-queue-id sqs-client queue-name)

        queue-id (if (and (:auto-create-queue? queue-configuration) (not exist-queue-id))
                   (queue/create-queue sqs-client queue-name)
                   exist-queue-id)

        queue-response (messaging/receive-message sqs-client queue-id listener-configuration)

        queue-response-type (sqs-handle-response-type (::sqs/response-type listener-configuration))

        interceptors (or (::sqs/response-interceptors listener-configuration) [queue-response-type])]

    (if queue-response
      ;; reference in https://github.com/cognitect-labs/pedestal.kafka/blob/master/src/com/cognitect/kafka/consumer.clj#L120
      (sqs-handle-deletion-policy
        (interceptor.chain/execute
          (-> service-map
              (assoc :message (first queue-response)
                     :queue-id queue-id
                     :deletion-policy (::sqs/deletion-policy listener-configuration)
                     :sqs-client sqs-client
                     :queue-fn queue-fn))
          interceptors))
      nil)))

(defn- starter
  [service-map]
  (let [sqs-client (queue/create-sqs-client (::sqs/client service-map))
        listeners (::sqs/listeners service-map)

        service-map-with-sqs (-> service-map
                                 (assoc :sqs/components {:client sqs-client})
                                 (dissoc service-map ::sqs-start-fn))]

    (log/info :sqs "Starting listener SQS queues")

    ;; reference in https://github.com/cognitect-labs/pedestal.kafka/blob/master/src/com/cognitect/kafka.clj#L43
    ;; other reference in https://github.com/spring-cloud/spring-cloud-aws/blob/v2.0.0.M4/spring-cloud-aws-messaging/src/main/java/org/springframework/cloud/aws/messaging/listener/SimpleMessageListenerContainer.java#L279
    (doseq [listener listeners]
      (log/info :sqs (str "SQS queue register '" (listener 0) "'"))
      (a/go-loop []
        (sqs-start-listener sqs-client listener service-map-with-sqs)
        (recur)))

    (let [bootstrapped-service-map (bootstrap/default-interceptors service-map)
          default-interceptors (::bootstrap/interceptors bootstrapped-service-map)
          interceptor-with-sqs {:name  ::sqs-components
                                :enter (fn [context]
                                         (assoc-in context [:request :sqs-client] sqs-client))}]

      (assoc
        bootstrapped-service-map
        ::bootstrap/interceptors
        (into default-interceptors [(interceptor/interceptor interceptor-with-sqs)])))))

;; Core functions

(def start (sqs/service-fn ::sqs-start-fn))

(defn sqs-server
  [service-map]
  (assoc service-map ::sqs-start-fn starter))
