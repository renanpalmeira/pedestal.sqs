;; REFERENCE IN https://github.com/cognitect-labs/pedestal.kafka/blob/91e826112b2f2bdc6a366a66b6a3cc07f7fca20b/src/com/cognitect/kafka/parser.clj
(ns pedestal.sqs.interceptors
  (:require [cheshire.core :as json]
            [io.pedestal.interceptor :as interceptor]
            [cognitect.transit :as transit])
  (:import (java.io PushbackReader StringReader ByteArrayInputStream)
           (java.util Base64)))

(defn- json-parser-value
  [body]
  (json/parse-stream (-> body StringReader. PushbackReader.) true))

(defn- transit-json-parser-value
  [^String body]
  (transit/read (transit/reader (ByteArrayInputStream. (.getBytes body)) :json)))

;; reference in https://github.com/99Taxis/common-sqs/blob/master/src/main/scala/com/taxis99/amazon/serializers/MsgPack.scala#L35
(defn- transit-msgpack-parser-value
  [^String body]
  (-> (.decode (Base64/getDecoder) body)
      (ByteArrayInputStream.)
      (#(transit/read (transit/reader % :msgpack)))))

(defn parse-message-with
  [parser-fn]
  (interceptor/interceptor
    {:name ::value-parser
     :enter
           (fn [context]
             (assoc-in context [:message :Body]
                       (parser-fn ^String (get-in context [:message :Body]))))}))

(def json-parser (parse-message-with json-parser-value))
(def transit-json-parser (parse-message-with transit-json-parser-value))
(def transit-msgpack-parser (parse-message-with transit-msgpack-parser-value))
(def string-parser (parse-message-with str))