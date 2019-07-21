(defproject pedestal.sqs "1.0.0-alpha4"
  :description "Pedestal interface for AWS SQS."
  :url "https://github.com/renanpalmeira/pedestal.sqs"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"
            :distribution :repo}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/core.async "0.4.500"]
                 [org.clojure/test.check "0.10.0-RC1"]

                 [io.pedestal/pedestal.service "0.5.7"]
                 [io.pedestal/pedestal.log "0.5.7"]
                 [io.pedestal/pedestal.route "0.5.7"]
                 [io.pedestal/pedestal.interceptor "0.5.7"]

                 [javax.servlet/javax.servlet-api "3.1.0"]

                 [com.cognitect.aws/api "0.8.345" :exclusions [[org.eclipse.jetty/jetty-util]]]
                 [com.cognitect.aws/endpoints "1.1.11.586"]
                 [com.cognitect.aws/sqs "697.2.391.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/data.xml "0.2.0-alpha6"]

                 [cheshire "5.8.1"]
                 [com.cognitect/transit-clj "0.8.313"]

                 [org.eclipse.jetty/jetty-util "9.4.18.v20190429"]]
  :min-lein-version "2.0.0"

  :deploy-repositories [["clojars" {:sign-releases false
                                    :url "https://clojars.org/repo"}]]

  :global-vars {*warn-on-reflection* true}

  :main pedestal.sqs
  :source-paths ["src"]

  :profiles {:dev     {:aliases      {"integration-test" ["sqs" "test"]}
                       :plugins      [[lein-sqs "0.1.0"]]
                       :dependencies [[io.pedestal/pedestal.service-tools "0.5.7"]]}
             :uberjar {:aot :all}})
