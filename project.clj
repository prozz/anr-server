(defproject netrunner "0.1"
  :description "netrunner game server"
  :url "http://example.com/FIXME"
  :plugins [[lein-ring "0.8.8"]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.taoensso/timbre "3.0.0-RC1"]
                 [compojure "1.1.6"]
                 [cheshire "5.2.0"]
                 [clj-http "0.7.7"]
                 [clj-time "0.6.0"]
                 [ring "1.2.1"]
                 [slingshot "0.10.3"]
                 [org.clojure/tools.trace "0.7.6"]]
  :ring {:handler netrunner.core/web-handler
         :port 8899}
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                                  [ring-mock "0.1.5"]]}}
  :aot :all)



