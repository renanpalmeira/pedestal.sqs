(ns pedestal.sqs.listener
  (:require [clojure.core.async :as a]
            [cognitect.aws.client.api :as aws]
            [pedestal.sqs.sqs :as sqs]))

;; Utility AWS

(defn delete-msg [client queue-id receipt-id]
  (aws/invoke client {:op      :DeleteMessage
                      :request {:QueueUrl      queue-id
                                :ReceiptHandle receipt-id}}))

(defn get-id [client queue-name]
  (let [resp (aws/invoke client {:op      :GetQueueUrl
                                 :request {:QueueName queue-name}})]
    (:QueueUrl resp)))

(defn create-queue [client queue-name]
  (let [resp (aws/invoke client {:op      :CreateQueue
                                 :request {:QueueName queue-name}})]
    (:QueueUrl resp)))

(defn- receive-msg
  [client queue-id opts]
  (let [resp (aws/invoke client {:op      :ReceiveMessage
                                 :request (merge opts
                                                 {:QueueUrl queue-id})})
        messages (:Messages resp)]
    messages))

;; Utility listener

(defn sqs-start-listener
  [sqs-client listener queue-configuration]
  (let [queue-name (listener 0)
        queue-fn (listener 1)
        listener-configuration (get listener 2 {})

        exist-queue-id (get-id sqs-client queue-name)

        queue-id (if (and (:auto-create-queue? queue-configuration) (not exist-queue-id))
                   (create-queue sqs-client queue-name)
                   exist-queue-id)

        queue-response (receive-msg sqs-client queue-id listener-configuration)]

    (if queue-response
      (queue-fn (first queue-response))
      nil)))

(defn- starter
  [service-map]
  (let [sqs-client (aws/client (merge (::sqs/client service-map) {:api :sqs}))
        sqs-configurations (::sqs/configurations service-map {})
        listeners (::sqs/listeners service-map)]

    ;; reference in https://github.com/cognitect-labs/pedestal.kafka/blob/master/src/com/cognitect/kafka.clj#L43
    ;; other reference in https://github.com/spring-cloud/spring-cloud-aws/blob/v2.0.0.M4/spring-cloud-aws-messaging/src/main/java/org/springframework/cloud/aws/messaging/listener/SimpleMessageListenerContainer.java#L279
    (doseq [listener listeners]
      (a/go-loop []
        (sqs-start-listener sqs-client listener sqs-configurations)
        (recur)))

    (dissoc service-map ::sqs-start-fn)))

;; Core functions

(def start (sqs/service-fn ::sqs-start-fn))

(defn sqs-server
  [service-map]
  (assoc service-map ::sqs-start-fn starter))

