(ns pedestal.sqs.queue
  (:require [cognitect.aws.client.api :as aws]
            [io.pedestal.log :as log]))

;; Utility AWS SQS Queue

(defn create-sqs-client
  [sqs-client-properties]
  (aws/client (merge sqs-client-properties {:api :sqs})))

(defn get-queue-id
  [client queue-name]
  (let [resp (aws/invoke client {:op      :GetQueueUrl
                                 :request {:QueueName queue-name}})]
    (:QueueUrl resp)))

(defn create-queue
  [client queue-name]
  (let [resp (aws/invoke client {:op      :CreateQueue
                                 :request {:QueueName queue-name}})]
    (log/info :sqs (str "Queue '" queue-name "' created"))
    (:QueueUrl resp)))