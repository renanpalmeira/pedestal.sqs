(ns pedestal.sqs.messaging
  (:require [cognitect.aws.client.api :as aws]
            [cheshire.core :as json]
            [cognitect.transit :as transit])
  (:import (java.io OutputStream ByteArrayOutputStream)
           (java.util Base64)))

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

(defn- ^String write-transit
  [^OutputStream stream type body]
  (let [_    (transit/write (transit/writer stream type) body)
        ret  (.toString stream)]
    (.close stream)
    ret))

;; reference in https://github.com/99Taxis/common-sqs/blob/master/src/main/scala/com/taxis99/amazon/serializers/MsgPack.scala#L26
(defn ^String to-transit-msgpack
  ([body]
   (to-transit-msgpack (ByteArrayOutputStream.) body))
  ([^ByteArrayOutputStream out body]
   (transit/write (transit/writer out :msgpack) body)
   (.close out)
   (.encodeToString (Base64/getEncoder) (.toByteArray out))))

(defn ^String to-transit-json
  ([body]
   (to-transit-json (ByteArrayOutputStream.) body))
  ([^OutputStream out body]
   (write-transit out :json body)))