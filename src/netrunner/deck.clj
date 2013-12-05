(ns netrunner.deck
  (:require [netrunner.cards :refer :all]
            [clj-http.client :as http]
            [slingshot.slingshot :refer [throw+]]
            [clojure.string :as s :refer [split-lines]]))

(defn download-deck [netrunnerdb-id]
  (let [response (http/get (str "http://netrunnerdb.com/decklist/export/text/" netrunnerdb-id)) 
        status (:status response)]
    (if (not= 200 status)
      (throw+ {:message (str "cannot download deck " netrunnerdb-id " from web") :status status}))
    (:body response)))

(defn parse-deck [buf]
  (let [lines  (split-lines buf)
        ; parse cards
        cards (map title->id lines)
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
    (if (not (empty? deck)) 
      {:identity identity :cards deck})))

;; queries for deck

(defn count-deck [deck]
  (count (:cards deck)))

(defn shuffle-deck [deck]
  (assoc deck :cards (shuffle (:cards deck))))

(defn get-identity [deck]
  (:identity deck))


;; queries for identity

(defn get-minimum-decksize [card-id]
  (get-in @db [card-id :minimumdecksize]))

(defn get-influence-limit [card-id]
  (get-in @db [card-id :influencelimit]))


;; queries for all cards
(defn get-title [card-id]
  (get-in @db [card-id :title]))
              
(defn get-faction [card-id]
  (get-in @db [card-id :faction_code]))

(defn get-faction-influence [card-id]
  (get-in @db [card-id :factioncost]))

(defn faction? [faction card-id]
  (= faction (get-faction card-id)))

(defn same-faction? [& card-ids]
  (apply = (map get-faction card-ids)))

(defn valid-deck? [deck]
  (let [deck-id (get-identity deck)
        non-faction-cards (remove (partial same-faction? deck-id) (:cards deck))
        influence-count (reduce + (map get-faction-influence non-faction-cards))]
    (and 
      (>= (get-minimum-decksize deck-id) (count-deck deck))
      (<= (get-influence-limit deck-id) influence-count))))

(comment
  (parse-deck (slurp "resources/ct.deck" :encoding "UTF-8"))
  (= 40 (count (parse-deck (slurp "resources/ct.deck" :encoding "UTF-8"))))
  (shuffle-deck (parse-deck (slurp "resources/ct.deck" :encoding "UTF-8")))
  (true? (validate-deck (parse-deck (slurp "resources/ct.deck" :encoding "UTF-8"))))
  (false? (validate-deck (parse-deck (slurp "resources/ct-invalid.deck" :encoding "UTF-8"))))
  (id->title 4027)
)
