(ns netrunner.deck
  (:require
            [netrunner.util :refer :all]
            [netrunner.cards :refer :all]
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

(defn deck-key [side]
  "key for use in games state map"
  (keyword (str (name side) "_deck")))

(defn load-deck
  ([side]
     (let [file (case side
                  :corp  "resources/core-set-weyland.deck"
                  :runner "resources/core-set-gabe.deck")]
       (parse-deck (slurp file :encoding "UTF-8"))))
  ([game side]
     (assoc game (deck-key side) (load-deck side)))
  ([game side netrunnerdb-id]
     (let [deck (parse-deck (download-deck netrunnerdb-id))]
       (assoc game (deck-key side) deck))))

(defn get-identity [deck]
  (:identity deck))

(defn count-deck
  ([deck] (count (:cards deck)))
  ([game side] (count-deck (get-in game [(deck-key side)]))))

(defn get-minimum-decksize [card-id]
  (get-in @db [card-id :minimumdecksize]))

(defn get-influence-limit [card-id]
  (get-in @db [card-id :influencelimit]))

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

(defn count-influence [deck]
  (let [deck-identity (get-identity deck)
        non-faction-cards (remove (partial same-faction? deck-identity) (:cards deck))]
    (reduce + (map get-faction-influence non-faction-cards))))

(defn valid-deck? [deck]
  (and
    (>= (get-minimum-decksize (get-identity deck)) (count-deck deck))
    (<= (get-influence-limit (get-identity deck)) (count-influence deck))))

;; deck operations
(defn shuffle-deck
  ([deck] (update-in deck [:cards] (comp seq shuffle)))
  ([game side] (update-in game [(deck-key side)] shuffle-deck)))

(defn peek-at-cards
  ([deck n]
     (take n (:cards deck)))
  ([game side n]
     (peek-at-cards ((deck-key side) game) n)))

(defn peek-at-top-card
  ([deck] (peek-at-cards deck 1))
  ([game side] (peek-at-cards game side 1)))

(defn put-top-card-at-the-bottom
  ([deck] (update-in deck [:cards] rotate-forward))
  ([game side] (update-in game [(deck-key side)] put-top-card-at-the-bottom)))

(defn put-cards-on-top
  ([deck cards] (update-in deck [:cards] (partial concat cards)))
  ([game side cards] (update-in game [(deck-key side)] put-cards-on-top cards)))

(defn put-cards-on-bottom
  ([deck cards] (update-in deck [:cards] concat cards))
  ([game side cards] (update-in game [(deck-key side)] put-cards-on-bottom cards)))
