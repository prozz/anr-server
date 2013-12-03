(ns netrunner.deck
  (:require [netrunner.cards :refer :all]
            [clojure.string :as s :refer [split-lines]]))

(defn parse-deck [db buf]
  (let [lines  (split-lines buf)]
  (map (partial search-for-id db) lines)
    ))

(comment
  (parse-deck (generate-db) (slurp "resources/ct.deck"))
  (id->title (generate-db) 4027)
)
