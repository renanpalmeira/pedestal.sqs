(ns pedestal.sqs.sqs
  (:require [clojure.core.async :as a]
            [clojure.spec.alpha :as s]
            [cognitect.aws.client.api :as aws]))


(s/def ::sqs-start-fn fn?)

(s/def ::client map?)
(s/def ::configurations map?)
(s/def ::listeners any?)

;; reference in https://github.com/cognitect-labs/pedestal.kafka/blob/master/src/com/cognitect/kafka.clj#L19
(defmacro service-fn [k]
  `(fn [service-map#]
     (if-let [f# (get service-map# ~k)]
       (f# service-map#)
       service-map#)))