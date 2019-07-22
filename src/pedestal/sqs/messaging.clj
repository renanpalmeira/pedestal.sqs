(ns pedestal.sqs.messaging
  (:require [cognitect.aws.client.api :as aws]
            [cognitect.aws.client.api.async :as aws.async]
            [cheshire.core :as json]))

;; Utility AWS SQS Messaging queue

(defn delete-message
  [client queue-id receipt-id]
  (aws/invoke client {:op      :DeleteMessage
                      :request {:QueueUrl      queue-id
                                :ReceiptHandle receipt-id}}))

(defn receive-message
  [client queue-id opts]
  (let [resp (aws/invoke client {:op      :ReceiveMessage
                                 :request (merge opts
                                                 {:QueueUrl queue-id})})
        messages (:Messages resp)]
    messages))

(defn send-message!
  [client queue-urls message]
  (doseq [queue-url (if (coll? queue-urls) queue-urls [queue-urls])]
    (aws/invoke client {:op      :SendMessage
                        :request {:QueueUrl    queue-url
                                  :MessageBody message}})))

;; Utility convert clojure to AWS SQS Messaging queue

(def to-json #(json/generate-string %))