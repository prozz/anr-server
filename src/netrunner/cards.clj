(ns netrunner.cards
  (:import [java.io FileNotFoundException])
  (:require [clj-http.client :as http]
            [clj-time.core :as time]
            [clj-time.format :refer [parse formatters]]
            [slingshot.slingshot :refer [throw+]]
            [clojure.pprint :as pp]
            [clojure.string :as s]))

(def max-db-age 24) ;hours

(defn download-db []
  (let [db (http/get "http://netrunnerdb.com/api/search/d:c|r" {:as :json})
        status (:status db)]
    (if (not= 200 status)
      (throw+ {:message "cannot download the cards from web" :status status}))
    (spit "/tmp/netrunner.cards" db)))

(defn load-db 
  ([] (load-db false))
  ([retry]
    (try
        (read-string (slurp "/tmp/netrunner.cards"))
      (catch FileNotFoundException e
        (if retry 
          (throw+ {:message "cannot load db, even after retried download" :ex e}))
        (download-db)
        (load-db true)))))

(defn- date-from-headers [db date-key]
  ; rfc822 vs Mon, 02 Dec 2013 22:16:42 +0000 vs Wed, 20 Nov 2013 09:57:08 GMT
  ; fix to clj-time needed, see https://github.com/clj-time/clj-time/issues/97
  (let [time (get-in db [:headers date-key])
        fixed-time (s/replace time "GMT" "+0000")]
    (parse (formatters :rfc822) fixed-time)))

(defn last-downloaded [db]
  (date-from-headers db "date"))

(defn last-modified [db]
  (date-from-headers db "last-modified"))

(defn db-age [db]
  (time/in-hours (time/interval (last-downloaded db) (time/now))))

(defn refresh-needed? [db]
  (< max-db-age (db-age db)))

(defn- code->int [code]
  "code has leading zeros, for example 00507 is read-string as 327, we want to avoid that and let it be 507"
  (read-string (s/replace code #"^(0*)(\d+)$" "$2")))

(defn generate-db []
  (let [db (load-db)]
    (if (refresh-needed? db)
      (do 
        (println "refreshing db")
        (download-db)
        (generate-db))
      (into {} (map (juxt (comp code->int :code) identity) (:body db))))))

(defn title->id [db title]
  (letfn [(normalize [s] (-> s s/trim s/lower-case (s/replace #"\s+|\n+" " ")))
          (pattern [s] (re-pattern (str "^(.*)?" (normalize s) "(.*)?$")))]
    (let [results (filter #(re-find (pattern (:title %)) (normalize title)) (vals db))]
      (if (not (empty? results))
        (-> results
          first
          :code
          code->int)))))

(defn id->title [db id]
  (:title (get db id)))

(comment
  re regenerate db run following function
  (generate-db)
  (title->id (generate-db) "sure gamble")
  (title->id (generate-db) "chaos theory: wünderkind")
)
