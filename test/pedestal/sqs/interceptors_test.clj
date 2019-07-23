(ns pedestal.sqs.interceptors-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [cognitect.aws.credentials :as credentials]
            [cognitect.aws.retry :as aws.retry]
            [cognitect.aws.client.api :as aws]
            [pedestal.sqs :as sqs]
            [pedestal.sqs.queue :as queue]
            [pedestal.sqs.messaging :as messaging]
            [pedestal.sqs.listener :as listener]))

;; SETUP

(def test-service-map
  {::sqs/client    {:region               "us-east-1"
                    :backoff              (aws.retry/capped-exponential-backoff 100 20000 0)
                    :retriable?           (fn [_] false)
                    :credentials-provider (credentials/basic-credentials-provider
                                            {:access-key-id     "a"
                                             :secret-access-key "b"})
                    :endpoint-override    {:protocol :http
                                           :hostname "localhost"
                                           :port     8084}}
   ::sqs/listeners (gen/generate (s/gen (s/coll-of ::sqs/listener)))})

;; UTILS

(def fake-sqs-client (queue/create-sqs-client (::sqs/client test-service-map)))

(defn- get-queues-in-fake-sqs
  [client]
  (:QueueUrls (aws/invoke client {:op :ListQueues}) []))

(defn- clear-queues-in-fake-sqs
  [client]
  (doseq [queue (get-queues-in-fake-sqs client)]
    (aws/invoke client {:op      :DeleteQueue
                        :request {:QueueUrl queue}})))

;; FIXTURES

(defn sqs-fake-fixture [f]
  (f)
  (clear-queues-in-fake-sqs fake-sqs-client))

(use-fixtures :each sqs-fake-fixture)

;; TESTS

(defn fake-json-listener-test
  [message]
  (is (= {:message "json"} (:Body (:message message)))))

(defn fake-transit-json-listener-test
  [message]
  (is (= {:message "transit-json"} (:Body (:message message)))))

(defn fake-transit-msgpack-listener-test
  [message]
  (is (= {:message "transit-msgpack"} (:Body (:message message)))))

(def listeners #{["test-queue-json" fake-json-listener-test {::sqs/deletion-policy :always
                                                             ::sqs/response-type   :json}]
                 ["test-queue-transit-json" fake-transit-json-listener-test {::sqs/deletion-policy :always
                                                                             ::sqs/response-type   :transit-json}]
                 ["test-queue-transit-msgpack" fake-transit-msgpack-listener-test {::sqs/deletion-policy :always
                                                                                   ::sqs/response-type   :transit-msgpack}]})

(deftest receive-a-message-test
  (let [test-queue-service-map (-> test-service-map
                                   (assoc ::sqs/configurations {:auto-create-queue? true
                                                                :auto-startup?      false})
                                   (assoc ::sqs/listeners listeners))]

    (testing "Startup with create test-queue"
      (let [service-map (-> test-queue-service-map
                            listener/sqs-server
                            listener/start
                            listener/stop)

            queues-in-fake-sqs (get-queues-in-fake-sqs (:sqs-client service-map))]

        (is (= 3 (count queues-in-fake-sqs)))))

    (testing "Publish a json message"
      (messaging/send-message!
        fake-sqs-client
        (queue/get-queue-id fake-sqs-client "test-queue-json")
        (messaging/to-json {:message "json"})))

    (testing "Receive a json message"
      (let [server (-> test-queue-service-map
                       (assoc ::sqs/configurations {:auto-create-queue? true})
                       listener/sqs-server
                       listener/start)]
        (Thread/sleep (* 10 1000)) ;; ugly but necessary to test receive message (see fake-listener-test)
        (listener/stop server)))



    (testing "Publish a transit-json message"
      (messaging/send-message!
        fake-sqs-client
        (queue/get-queue-id fake-sqs-client "test-queue-transit-json")
        (messaging/to-transit-json {:message "transit-json"})))

    (testing "Receive a transit-json message"
      (let [server (-> test-queue-service-map
                       (assoc ::sqs/configurations {:auto-create-queue? true})
                       listener/sqs-server
                       listener/start)]
        (Thread/sleep (* 10 1000)) ;; ugly but necessary to test receive message (see fake-listener-test)
        (listener/stop server)))

    (testing "Publish a transit-msgpack message"
      (messaging/send-message!
        fake-sqs-client
        (queue/get-queue-id fake-sqs-client "test-queue-transit-msgpack")
        (messaging/to-transit-msgpack {:message "transit-msgpack"})))

    (testing "Receive a transit-msgpack message"
      (let [server (-> test-queue-service-map
                       (assoc ::sqs/configurations {:auto-create-queue? true})
                       listener/sqs-server
                       listener/start)]
        (Thread/sleep (* 10 1000)) ;; ugly but necessary to test receive message (see fake-listener-test)
        (listener/stop server)))))