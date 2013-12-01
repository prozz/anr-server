(defproject netrunner "0.1-SNAPSHOT"
  :description "netrunner game server"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.taoensso/timbre "3.0.0-RC1"]
                 [compojure "1.1.6"]
                 [cheshire "5.2.0"]]
  :plugins [[lein-ring "0.8.8"]
            [lein-sha-version "0.1.1"]]
  :ring {:handler netrunner.core/web-handler
         :port 8899}
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring-mock "0.1.5"]]}}
  :aot :all)

