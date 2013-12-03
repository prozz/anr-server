(ns netrunner.core
  (:use compojure.core)
  (:use ring.adapter.jetty)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
	          [netrunner.lobby :as lobby]
            [taoensso.timbre :as timbre]))

(timbre/refer-timbre)

(defn spawn []
  { :lobby (lobby/init) })

(defroutes app-routes
  (GET "/" [] (str "Netrunner Game Server"))
  (route/resources "/")
  (route/not-found "Not Found"))

(def web-handler
  (handler/site app-routes))
