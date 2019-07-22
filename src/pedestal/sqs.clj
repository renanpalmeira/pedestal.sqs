(ns pedestal.sqs
  (:gen-class)
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))


(s/def ::sqs-start-fn fn?)
(s/def ::sqs-stop-fn fn?)

(s/def ::client map?)
(s/def ::configurations map?)

(defn- gen-queue-fn
  []
  (gen/return identity))

(s/def ::queue-fn (s/spec fn?
                          :gen gen-queue-fn))

(s/def ::listener (s/cat :queue-name (s/and
                                       string?
                                       #(not (empty? %))
                                       #(not (clojure.string/blank? %)))
                         :queue-fn ::queue-fn
                         :queue-configurations map?))

;; reference in https://stackoverflow.com/questions/46135111/how-to-check-distinct-id-in-spec-coll-of?answertab=votes#tab-top
(s/def ::listeners (s/and
                     (s/coll-of ::listener)
                     #(if (empty? %) true (apply distinct? (mapv :queue-name %)))))

(s/def ::service-map-in (s/keys :req [::listeners ::client]
                                :opt [::configurations]))

;; reference in https://github.com/cognitect-labs/pedestal.kafka/blob/master/src/com/cognitect/kafka.clj#L19
(defmacro service-fn [k]
  `(fn [service-map#]
     (if-let [f# (get service-map# ~k)]
       (f# service-map#)
       service-map#)))