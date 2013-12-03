(ns netrunner.deck
  (:require [netrunner.cards :refer :all]
            [clojure.string :as s :refer [split-lines]]))

(defn parse-deck [db buf]
  (let [lines  (split-lines buf)
        ; parse cards
        cards (map (partial title->id db) lines)
        ; parse quantities
        quantities (map (partial re-find #"^\d") lines)
        ; make ["3" 2046] like pairs - quantity and id
        deck (map vector quantities cards)
        ; remove all [nil nil] pairs for parsed empty lines
        deck (remove (comp nil? last) deck)
        ; recognize [nil 2046] pair as identity
        identity (last (first (filter (comp nil? first) deck)))
        ; remove identity from deck
        deck (remove (comp nil? first) deck)
        ; replace ["3" 2046] with (2046, 2046, 2046)
        deck (map #(repeat (read-string (first %)) (last %)) deck)
        ; make deck flat list, not lists inside list
        deck (flatten deck)]
    {:identity identity :cards deck}))

(defn shuffle-deck [deck]
  (assoc deck :cards (shuffle (:cards deck))))

(comment
  (parse-deck (generate-db) (slurp "resources/ct.deck" :encoding "UTF-8"))
  (= 40 (count (parse-deck (generate-db) (slurp "resources/ct.deck" :encoding "UTF-8"))))
  (shuffle-deck (parse-deck (generate-db) (slurp "resources/ct.deck" :encoding "UTF-8")))
  (id->title (generate-db) 4027)
)
