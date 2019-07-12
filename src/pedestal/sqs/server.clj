(ns pedestal.sqs.server
  (:gen-class)                                              ; for -main method in uberjar
  (:require [io.pedestal.http :as server]
            [pedestal.sqs.service :as service]
            [pedestal.sqs.listener :as sqs-listener]))


(defn -main
  "The entry-point for 'lein run'"
  [& args]
  (println "\nCreating your server...")

  (-> service/service
      sqs-listener/sqs-server
      sqs-listener/start
      server/create-server
      server/start))

;; If you package the service up as a WAR,
;; some form of the following function sections is required (for io.pedestal.servlet.ClojureVarServlet).

;;(defonce servlet  (atom nil))
;;
;;(defn servlet-init
;;  [_ config]
;;  ;; Initialize your app here.
;;  (reset! servlet  (server/servlet-init service/service nil)))
;;
;;(defn servlet-service
;;  [_ request response]
;;  (server/servlet-service @servlet request response))
;;
;;(defn servlet-destroy
;;  [_]
;;  (server/servlet-destroy @servlet)
;;  (reset! servlet nil))

