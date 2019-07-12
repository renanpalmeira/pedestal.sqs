(ns pedestal.sqs.sqs
  (:require [clojure.core.async :as a]
            [clojure.spec.alpha :as s]
            [cognitect.aws.client.api :as aws]))


(s/def ::sqs-start-fn fn?)

(defmacro service-fn [k]
  `(fn [service-map#]
     (if-let [f# (get service-map# ~k)]
       (f# service-map#)
       service-map#)))

;; Utility AWS

(defn delete-msg [client queue-id receipt-id]
  (aws/invoke client {:op      :DeleteMessage
                      :request {:QueueUrl      queue-id
                                :ReceiptHandle receipt-id}}))

(defn get-id [client queue-name]
  (let [resp (aws/invoke client {:op      :GetQueueUrl
                                 :request {:QueueName queue-name}})]
    (:QueueUrl resp)))

(defn- receive-msg
  [client queue-id opts]
  (let [resp (aws/invoke client {:op      :ReceiveMessage
                                 :request (merge opts
                                                 {:QueueUrl queue-id})})
        messages (:Messages resp)
        ;acknowledgment (delete-msg client queue-id (:ReceiptHandle (first (:Messages resp))))
        ]
    ;(do acknowledgment)
    messages))

;; Core functions

(defn sqs-start-listener
  [sqs-client listener]
  (let [queue (listener 0)
        queue-fn (listener 1)
        queue-configuration (get listener 2 {})
        queue-response (receive-msg sqs-client (get-id sqs-client queue) queue-configuration)]

    (if queue-response
      (queue-fn (first queue-response))
      nil)))

(defn- starter
  [service-map]
  (let [sqs-client (aws/client (merge (::sqs/client service-map) {:api :sqs}))
        listeners (::sqs/listeners service-map)]

    (a/go-loop []
               (doseq [listener listeners]
                 (a/to-chan (sqs-start-listener sqs-client listener)))
               (recur))
    (dissoc service-map ::sqs-start-fn)))

(def start (service-fn ::sqs-start-fn))

(defn sqs-server
  [service-map]
  (assoc service-map ::sqs-start-fn starter))

