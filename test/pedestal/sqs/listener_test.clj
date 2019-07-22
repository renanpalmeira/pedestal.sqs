(ns pedestal.sqs.listener-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [cognitect.aws.client.api :as aws]
            [pedestal.sqs :as sqs]
            [pedestal.sqs.listener :as listener]
            [pedestal.sqs.queue :as queue]
            [pedestal.sqs.messaging :as messaging]
            [cognitect.aws.retry :as aws.retry]
            [cognitect.aws.credentials :as credentials]))

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
                                           :port     9324}}
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

(defn fake-listener-test
  [message]
  (is (= {:test "test"} (:Body (:message message)))))

(deftest receive-a-message-test
  (let [test-queue-service-map (-> test-service-map
                                   (assoc ::sqs/configurations {:auto-create-queue? true
                                                                :auto-startup? false})
                                   (assoc ::sqs/listeners #{["test-queue1" fake-listener-test {::sqs/deletion-policy :never
                                                                                               ::sqs/response-type   :json}]
                                                            ["test-queue2" fake-listener-test {::sqs/deletion-policy :always
                                                                                               ::sqs/response-type   :json}]
                                                            ["test-queue3" fake-listener-test {::sqs/deletion-policy :always
                                                                                               ::sqs/response-type   :json}]
                                                            ["test-queue4" fake-listener-test {::sqs/deletion-policy :always
                                                                                               ::sqs/response-type   :json}]
                                                            ["test-queue5" fake-listener-test {::sqs/deletion-policy :always
                                                                                               ::sqs/response-type   :json}]
                                                            ["test-queue6" fake-listener-test {::sqs/deletion-policy :always
                                                                                               ::sqs/response-type   :json}]
                                                            ["test-queue7" fake-listener-test {::sqs/deletion-policy :always
                                                                                               ::sqs/response-type   :json}]
                                                            ["test-queue8" fake-listener-test {::sqs/deletion-policy :always
                                                                                               ::sqs/response-type   :json}]
                                                            ["test-queue9" fake-listener-test {::sqs/deletion-policy :always
                                                                                               ::sqs/response-type   :json}]
                                                            ["test-queue10" fake-listener-test {::sqs/deletion-policy :always
                                                                                                ::sqs/response-type   :json}]
                                                            ["test-queue11" fake-listener-test {::sqs/deletion-policy :always
                                                                                                ::sqs/response-type   :json}]
                                                            ["test-queue12" fake-listener-test {::sqs/deletion-policy :always
                                                                                                ::sqs/response-type   :json}]
                                                            ["test-queue13" fake-listener-test {::sqs/deletion-policy :always
                                                                                                ::sqs/response-type   :json}]
                                                            ["test-queue14" fake-listener-test {::sqs/deletion-policy :always
                                                                                                ::sqs/response-type   :json}]
                                                            ["test-queue15" fake-listener-test {::sqs/deletion-policy :always
                                                                                                ::sqs/response-type   :json}]
                                                            ["test-queue16" fake-listener-test {::sqs/deletion-policy :always
                                                                                                ::sqs/response-type   :json}]
                                                            ["test-queue17" fake-listener-test {::sqs/deletion-policy :always
                                                                                                ::sqs/response-type   :json}]
                                                            ["test-queue18" fake-listener-test {::sqs/deletion-policy :always
                                                                                                ::sqs/response-type   :json}]
                                                            ["test-queue19" fake-listener-test {::sqs/deletion-policy :always
                                                                                                ::sqs/response-type   :json}]
                                                            ["test-queue20" fake-listener-test {::sqs/deletion-policy :always
                                                                                                ::sqs/response-type   :json}]}))]

    (testing "Startup with create test-queue"
      (let [service-map (-> test-queue-service-map
                            listener/sqs-server
                            listener/start
                            listener/stop)

            queues-in-fake-sqs (get-queues-in-fake-sqs (:sqs-client service-map))]

        (is (= 20 (count queues-in-fake-sqs)))))

    (testing "Publish a json message"
      (messaging/send-message!
        fake-sqs-client
        (queue/get-queue-id fake-sqs-client "test-queue1")
        (messaging/to-json {:test "test"})))

    (testing "Receive a json message"
      (let [server (-> test-queue-service-map
                       (assoc ::sqs/configurations {:auto-create-queue? true})
                       listener/sqs-server
                       listener/start)]
        (Thread/sleep (* 10 1000)) ;; ugly but necessary to test receive message (see fake-listener-test)
        (listener/stop server)))))

(deftest listener-with-auto-create-queue-turn-on-test
  (testing "Startup with {:auto-create-queue? true}"
    (let [service-map (-> test-service-map
                          (assoc ::sqs/configurations {:auto-create-queue? true
                                                       :auto-startup? false})
                          listener/sqs-server
                          listener/start
                          listener/stop)

          queues-in-fake-sqs (get-queues-in-fake-sqs (:sqs-client service-map))]

      (is (= (count (::sqs/listeners test-service-map)) (count queues-in-fake-sqs))))))

(deftest listener-with-auto-create-queue-turnoff-test
  (testing "Startup with {:auto-create-queue? false}"
    (let [service-map (-> test-service-map
                          (assoc ::sqs/configurations {:auto-create-queue? false
                                                       :auto-startup? false})
                          listener/sqs-server
                          listener/start
                          listener/stop)

          queues-in-fake-sqs (get-queues-in-fake-sqs (:sqs-client service-map))]

      (is (= 0 (count queues-in-fake-sqs)))))

  (testing "Startup with without set :auto-create-queue?"
    (let [service-map (-> test-service-map
                          (assoc ::sqs/configurations {:auto-startup? false})
                          listener/sqs-server
                          listener/start
                          listener/stop)

          queues-in-fake-sqs (get-queues-in-fake-sqs (:sqs-client service-map))]

      (is (= 0 (count queues-in-fake-sqs))))))
