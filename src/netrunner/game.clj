(ns netrunner.game
  (:use [netrunner.cards :refer :all]
        [netrunner.deck :refer :all]
        [netrunner.lobby :refer :all]
        [netrunner.util :refer :all]
        [midje.sweet]))

(defn game-ready? [game]
  (let [corp (get-in game [:corp :deck])
        runner (get-in game [:runner :deck])]
    (every? identity (map not-empty [corp runner]))))

(fact "game is ready when decks of both players are loaded"
      (game-ready? {}) => false
      (game-ready? {:runner {:deck {}}}) => false
      (game-ready? {:runner {:deck {:id 1}}}) => false
      (game-ready? {:runner {:deck {}} :corp {:deck {}}}) => false
      (game-ready? {:runner {:deck {:id 1}} :corp {:deck {:id 1}}}) => true)

(defn starting-clicks [side]
  ({:runner 4 :corp 3} side))

(fact "corp has 3 and runner has 4 starting clicks"
      (starting-clicks :corp) => 3
      (starting-clicks :runner) => 4)

(defn start-game [game]
  "if both decks are loaded prepares all needed game related entities:
   - hq/grip,
   - archives/heap,
   - clicks
   - credits
   - tags
   - bad pubs
  then shuffles decks and draws starting hands"
  (if (game-ready? game)
    (-> game
        (deep-merge {:runner {:clicks 4 :credits 5 :hand '() :discard '() :tags 0}})
        (deep-merge {:corp   {:clicks 3 :credits 5 :hand '() :discard '() :bad-pubs 0}})
        (shuffle-deck :runner)
        (shuffle-deck :corp))))

(fact "created game doesnt contain decks"
      (let [game (-> {} (create-game "john" :corp) (join-game "barry"))]
        (get-in game [:corp :deck]) => nil
        (get-in game [:runner :deck]) => nil))
(fact "created game cannot be started without decks loaded"
      (let [game (-> {} (create-game "john" :corp) (join-game "barry"))]
        (start-game game) => nil))

(fact "started game contains both decks"
      (let [game (-> {} (load-deck :corp) (load-deck :runner) (start-game))]
        (get game :corp) => (contains {:deck anything})
        (get game :runner) => (contains {:deck anything})))

(fact "started game has deck shuffled"
      (let [game (-> {} (load-deck :corp) (load-deck :runner))
            corp-deck (get-in game [:corp :deck])
            runner-deck (get-in game [:runner :deck])]
        (get-in game [:corp :deck]) => corp-deck
        (get-in (start-game game) [:corp :deck]) =not=> corp-deck
        (get-in game [:runner :deck]) => runner-deck
        (get-in (start-game game) [:runner :deck]) =not=> runner-deck))

(defn start-turn [game side]
  (-> game
      (assoc-in [side :clicks] (starting-clicks side))
      (assoc :current_turn side)
      ;#(if (= :corp side) (draw-card % side) %)
      ))

(defn draw-card [game side]
  (-> game
      (update-in [side :hand] concat (peek-at-top-card game side))
      (update-in [side :deck :cards] rest)))

(defn draw-cards [game side n]
  (if (zero? n)
    game
    (draw-cards (draw-card game side) side (- n 1))))

(facts "drawing cards"
       (draw-card {:corp {:deck {:cards '(1 2 3)} :hand '()}} :corp) => {:corp {:deck {:cards '(2 3)} :hand '(1)}}
       (draw-card {:corp {:deck {:cards '(2 3)} :hand '(1)}} :corp) => {:corp {:deck {:cards '(3)} :hand '(1 2)}}
       (draw-card {:corp {:deck {:cards '()} :hand '()}} :corp) => {:corp {:deck {:cards '()} :hand '()}}
       (draw-card {:corp {:deck {:cards '()} :hand '(1 2 3)}} :corp) => {:corp {:deck {:cards '()} :hand '(1 2 3)}}
       (draw-cards {:corp {:deck {:cards '(1 2 3)} :hand '()}} :corp 2) => {:corp {:deck {:cards '(3)} :hand '(1 2)}}
       (draw-cards {:corp {:deck {:cards '(1 2 3)} :hand '()}} :corp 5) => {:corp {:deck {:cards '()} :hand '(1 2 3)}}
       (draw-cards {:corp {:deck {:cards '()} :hand '()}} :corp 3) => {:corp {:deck {:cards '()} :hand '()}}
       (draw-cards {:corp {:deck {:cards '()} :hand '(1 2 3)}} :corp 3) => {:corp {:deck {:cards '()} :hand '(1 2 3)}})

(defn discard-hand [game side]
  (-> game
      (update-in [side :discard] concat (get-in game [side :hand]))
      (assoc-in [side :hand] '())))

(facts "discarding hand"
       (discard-hand {:corp {:hand '() :discard '()}} :corp) => {:corp {:hand '() :discard '()}}
       (discard-hand {:corp {:hand '(1 2) :discard '()}} :corp) => {:corp {:hand '() :discard '(1 2)}}
       (discard-hand {:corp {:hand '(1 2 3) :discard '(4 5)}} :corp) => {:corp {:hand '() :discard '(4 5 1 2 3)}})

(defn in-hand? [game side card]
  "checks if card is in hand"
  (boolean (some #{card} (get-in game [side :hand]))))

(facts "cards in hand"
       (in-hand? {:corp {:hand '()}} :corp 1) => false
       (in-hand? {:corp {:hand '(1)}} :corp 1) => true
       (in-hand? {:corp {:hand '(1 2 3)}} :corp 2) => true
       (in-hand? {:corp {:hand '(1 2 2 1)}} :corp 2) => true)

(defn discard-card [game side card]
  (when (in-hand? game side card)
    (-> game
        (update-in [side :hand] (partial remove-first card))
        (update-in [side :discard] concat (list card)))))

(facts "discarding cards"
       (fact "cannot discard card that's not in hand" (discard-card {:runner {:hand '() :discard '()}} :runner 2) => nil)
       (fact "can do when it's in hand" (discard-card {:runner {:hand '(1 2 3) :discard '()}} :runner 2) => {:runner {:hand '(1 3) :discard '(2)}}))

(defn shuffle-discard-into-deck [game side]
  (-> game
      (update-in [side :deck :cards] concat (get-in game [side :discard]))
      (assoc-in [side :discard] '())
      (shuffle-deck side)))

(facts "shuffle discard pile into deck"
       (let [result (shuffle-discard-into-deck {:corp {:deck {:cards '(1 2 3)} :discard '(4 5)}} :corp)]
         (fact "discard is empty" result => (contains {:corp (contains {:discard empty?})}))
         (fact "deck is full again" result => (contains {:corp (contains {:deck (contains {:cards (contains [1 2 3 4 5] :in-any-order)})})}))))

(defn mulliganed? [game side]
  "check if side took mulligan"
  (boolean (get-in game [side :mulligan])))

(defn mulligan [game side]
  (when-not (mulliganed? game side)
    (-> game
        (discard-hand side)
        (shuffle-discard-into-deck side)
        (draw-cards side 5)
        (assoc-in [side :mulligan] true))))

(facts "mulligan"
       (let [game {:corp {:deck {:cards '(1 2 3 4 5)} :hand '(6 7 8 9 10)}}]
         (fact "marked as done" (mulligan game :corp) => (contains {:corp (contains {:mulligan true})}))
         (fact "cannot take mulligan twice" (-> game (mulligan :corp) (mulligan :corp)) => nil)
         (fact "after mulligan hand is new" (mulligan game :corp) => (contains {:corp (contains {:hand (five-of number?)})}))))

(defn tag-runner [game]
  (update-in game [:runner :tags] inc))

(defn remove-tag [game]
  (update-in-when pos? game [:runner :tags] dec))

(facts "tagging"
       (tag-runner {:runner {:tags 0}}) => {:runner {:tags 1}}
       (remove-tag {:runner {:tags 1}}) => {:runner {:tags 0}}
       (remove-tag {:runner {:tags 0}}) => nil)

(defn bad-pub-corp [game]
  (update-in game [:corp :bad-pubs] inc))

(defn remove-bad-pub [game]
  (update-in-when pos? game [:corp :bad-pubs] dec))

(facts "bad pubs"
       (bad-pub-corp {:corp {:bad-pubs 0}}) => {:corp {:bad-pubs 1}}
       (remove-bad-pub {:corp {:bad-pubs 1}}) => {:corp {:bad-pubs 0}}
       (remove-bad-pub {:corp {:bad-pubs 0}}) => nil)

(defn add-click [game side]
  (update-in game [side :clicks] inc))

(defn remove-click [game side]
  (update-in-when pos? game [side :clicks] dec))

(facts "clicks"
       (add-click {:runner {:clicks 0}} :runner) => {:runner {:clicks 1}}
       (remove-click {:runner {:clicks 1}} :runner) => {:runner {:clicks 0}}
       (remove-click {:runner {:clicks 0}} :runner) => nil)

(defn click-for-credit [game side])
(defn click-for-card [game side])
(defn click-for-resource-trash [game id])
(defn click-for-tag-removal [game])

(defn play [game card])
