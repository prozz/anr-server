(ns netrunner.deck
  (:use [midje.sweet])
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

(defn load-deck
  ([side]
     (let [file (case side
                  :corp  "resources/core-set-weyland.deck"
                  :runner "resources/core-set-gabe.deck")]
       (parse-deck (slurp file :encoding "UTF-8"))))
  ([game side]
     (assoc-in game [side :deck] (load-deck side)))
  ([game side netrunnerdb-id]
     (let [deck (parse-deck (download-deck netrunnerdb-id))]
       (assoc-in game [side :deck] deck))))

(defn get-identity [deck]
  (:identity deck))

(fact "default runner deck is gabe"
      (-> (load-deck :runner) get-identity id->title) => "Gabriel Santiago: Consummate Professional")
(fact "default corp decks is weyland"
      (-> (load-deck :corp) get-identity id->title) => "Weyland Consortium: Building a Better World")

(defn count-deck
  ([deck] (count (:cards deck)))
  ([game side] (count-deck (get-in game [side :deck]))))

(fact "default runner deck has 46 cards"
      (count-deck (load-deck :runner)) => 46)
(fact "default corp deck loaded as game has 49 cards"
      (count-deck (load-deck {} :corp) :corp) => 49)

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

(facts "deck properties"
       (let [deck (parse-deck (slurp "resources/ct.deck" :encoding "UTF-8"))
             deck-id (get-identity deck)
             femme-id (title->id "femme fatale")]
         (fact (get-title deck-id) => "Chaos Theory: Wünderkind")
         (fact (get-faction deck-id) => "shaper")
         (fact (get-minimum-decksize deck-id) => 40)
         (fact (get-influence-limit deck-id) => 15)
         (fact (same-faction? deck-id deck-id) => true)
         (fact (same-faction? deck-id femme-id) => false)
         (fact (get-faction-influence femme-id) => 1)))

(facts "deck validation"
       (let [deck (parse-deck (slurp "resources/ct.deck" :encoding "UTF-8"))]
         (fact (valid-deck? deck) => true)
         (fact (count-deck deck) => 40)
         (fact (count-influence deck) => 15))
       (let [deck (parse-deck (slurp "resources/ct-invalid.deck" :encoding "UTF-8"))]
         (fact (valid-deck? deck) => false)
         (fact (count-deck deck) => 42)
         (fact (count-influence deck) => 21)))

;; deck operations
(defn shuffle-deck
  ([deck] (update-in deck [:cards] (comp seq shuffle)))
  ([game side] (update-in game [side :deck] shuffle-deck)))

(facts "shuffling" (shuffle-deck (load-deck :runner)) =not=> (shuffle-deck (load-deck :runner)))

(defn peek-at-cards
  ([deck n]
     (take n (:cards deck)))
  ([game side n]
     (peek-at-cards (get-in game [side :deck]) n)))

(defn peek-at-top-card
  ([deck] (peek-at-cards deck 1))
  ([game side] (peek-at-cards game side 1)))

(facts "peeking"
       (peek-at-top-card {:cards '(1 2 3)}) => '(1)
       (peek-at-cards {:cards '(1 2 3)} 2) => '(1 2)
       (peek-at-top-card {:corp {:deck {:cards '(1 2 3)}} :runner {:deck {:cards '(4 5 6)}}} :runner) => '(4))

(defn put-top-card-at-bottom
  ([deck] (update-in deck [:cards] rotate-forward))
  ([game side] (update-in game [side :deck] put-top-card-at-bottom)))

(facts "putting at the bottom"
       (put-top-card-at-bottom {:cards '(1 2 3)}) => {:cards '(2 3 1)}
       (put-top-card-at-bottom {:corp {:deck {:cards '(1 2 3)}}} :corp) => {:corp {:deck {:cards '(2 3 1)}}})

(defn put-cards-on-top
  ([deck cards] (update-in deck [:cards] (partial concat cards)))
  ([game side cards] (update-in game [side :deck] put-cards-on-top cards)))

(facts "putting on top"
       (put-cards-on-top {:cards '(1 2 3)} '(4 5)) => {:cards '(4 5 1 2 3)}
       (put-cards-on-top {:corp {:deck {:cards '(1 2 3)}}} :corp '(4 5)) => {:corp {:deck {:cards '(4 5 1 2 3)}}})

(defn put-cards-at-bottom
  ([deck cards] (update-in deck [:cards] concat cards))
  ([game side cards] (update-in game [side :deck] put-cards-at-bottom cards)))

(facts "putting at bottom"
       (put-cards-at-bottom {:cards '(1 2 3)} '(4 5)) => {:cards '(1 2 3 4 5)}
       (put-cards-at-bottom {:corp {:deck {:cards '(1 2 3)}}} :corp '(4 5)) => {:corp {:deck {:cards '(1 2 3 4 5)}}})
