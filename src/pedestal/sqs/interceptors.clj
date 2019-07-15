;; REFERENCE IN https://github.com/cognitect-labs/pedestal.kafka/blob/91e826112b2f2bdc6a366a66b6a3cc07f7fca20b/src/com/cognitect/kafka/parser.clj
(ns pedestal.sqs.interceptors
  (:require [cheshire.core :as json]
            [io.pedestal.interceptor :as interceptor])
  (:import (java.io PushbackReader StringReader)))

(defn json-parser
  [value]
  (json/parse-stream (-> value StringReader. PushbackReader.) true))

(defn parse-message-with
  [parser-fn]
  (interceptor/interceptor
    {:name ::value-parser
     :enter
           (fn [context]
             (assoc-in context [:message :Body]
                       (parser-fn (get-in context [:message :Body]))))}))

(def json-parser (parse-message-with json-parser))
(def default-parser (parse-message-with str))