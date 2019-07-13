(ns pedestal.sqs.messaging
  (:require [cognitect.aws.client.api :as aws]))

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
  [client queue-url message]
  (aws/invoke client {:op      :SendMessage
                      :request {:QueueUrl    queue-url
                                :MessageBody message}}))