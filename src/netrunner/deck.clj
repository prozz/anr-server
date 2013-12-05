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

(defn get-minimum-decksize [db card-id]
  (get-in db [card-id :minimumdecksize]))

(defn get-influence-limit [db card-id]
  (get-in db [card-id :influencelimit]))


;; queries for all cards

(defn get-title [db card-id]
  (get-in db [card-id :title]))
              
(defn get-faction [db card-id]
  (get-in db [card-id :faction_code]))

(defn get-faction-influence [db card-id]
  (get-in db [card-id :factioncost]))

(defn faction? [db faction card-id]
  (= faction (get-faction db card-id)))

(defn same-faction? [db & card-ids]
  (apply = (map (partial get-faction db) card-ids)))

(defn valid-deck? [db deck]
  (let [deck-id (get-identity deck)
        non-faction-cards (remove (partial same-faction? db deck-id) (:cards deck))
        influence-count (reduce + (map (partial get-faction-influence db) non-faction-cards))]
    (println non-faction-cards)
    (println influence-count)
    (and 
      (>= (get-minimum-decksize db deck-id) (count-deck deck))
      (<= (get-influence-limit db deck-id) influence-count))))

(comment
  (get (generate-db) 4027)
  (parse-deck (generate-db) (slurp "resources/ct.deck" :encoding "UTF-8"))
  (= 40 (count (parse-deck (generate-db) (slurp "resources/ct.deck" :encoding "UTF-8"))))
  (shuffle-deck (parse-deck (generate-db) (slurp "resources/ct.deck" :encoding "UTF-8")))
  (true? (validate-deck (generate-db) (parse-deck (generate-db) (slurp "resources/ct.deck" :encoding "UTF-8"))))
  (false? (validate-deck (generate-db) (parse-deck (generate-db) (slurp "resources/ct-invalid.deck" :encoding "UTF-8"))))
  (id->title (generate-db) 4027)
)
