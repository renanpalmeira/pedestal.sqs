(defproject pedestal.sqs "0.0.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/core.async "0.4.500"]

                 [io.pedestal/pedestal.service "0.5.7"]
                 [io.pedestal/pedestal.interceptor "0.5.7"]

                 [com.cognitect.aws/api "0.8.345" :exclusions [[org.eclipse.jetty/jetty-util]]]
                 [com.cognitect.aws/endpoints "1.1.11.586"]
                 [com.cognitect.aws/sqs "697.2.391.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/data.xml "0.2.0-alpha6"]

                 [cheshire                         "5.8.1"]
                 [com.cognitect/transit-clj        "0.8.313"]

                 [org.eclipse.jetty/jetty-util "9.4.18.v20190429"]

                 ;; Remove this line and uncomment one of the next lines to
                 ;; use Immutant or Tomcat instead of Jetty:
                 [io.pedestal/pedestal.jetty "0.5.7"]
                 ;; [io.pedestal/pedestal.immutant "0.5.7"]
                 ;; [io.pedestal/pedestal.tomcat "0.5.7"]

                 [ch.qos.logback/logback-classic "1.2.3" :exclusions [org.slf4j/slf4j-api]]
                 [org.slf4j/jul-to-slf4j "1.7.26"]
                 [org.slf4j/jcl-over-slf4j "1.7.26"]
                 [org.slf4j/log4j-over-slf4j "1.7.26"]]
  :min-lein-version "2.0.0"
  :resource-paths ["config", "resources"]

  ;; If you use HTTP/2 or ALPN, use the java-agent to pull in the correct alpn-boot dependency
  ;:java-agents [[org.mortbay.jetty.alpn/jetty-alpn-agent "2.0.5"]]
  :profiles {:dev     {:aliases      {"run-dev" ["trampoline" "run" "-m" "pedestal.sqs.server/run-dev"]}
                       :dependencies [[io.pedestal/pedestal.service-tools "0.5.7"]]}
             :uberjar {:aot [pedestal.sample.server]}}
  :main ^{:skip-aot true} pedestal.sample.server)
