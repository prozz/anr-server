(ns netrunner.game
  (:use [netrunner.cards :refer :all]
        [netrunner.deck :refer :all]
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


(defn start-turn [game side]
  (-> game
      (assoc-in [side :clicks] (starting-clicks side))
      (assoc :current_turn side)
      ;#(if (= :corp side) (draw-card % side) %)
      ))

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
    (-> game ;use deep-merge instead of this crap here
        (assoc-in [:runner :clicks] 4)
        (assoc-in [:runner :credits] 5)
        (assoc-in [:runner :hand] '())
        (assoc-in [:runner :discard] '())
        (assoc-in [:runner :tags] 0)
        (assoc-in [:corp :clicks] 4)
        (assoc-in [:corp :credits] 5)
        (assoc-in [:corp :hand] '())
        (assoc-in [:corp :discard] '())
        (assoc-in [:corp :bad-pubs] 0)
        (shuffle-deck :runner)
        (shuffle-deck :corp))))

(def testing-game (-> {} (load-deck :corp) (load-deck :runner) (start-game)))

(facts "deck loading")
(comment
  (is (false? (contains? game :corp_deck)))
  (is (false? (contains? game :runner_deck)))
  (is (true? (contains? (load-deck game :corp) :corp_deck)))
  (is (true? (contains? (load-deck game :runner) :runner_deck))))

(facts "deck shuffling")
(comment
  (let [game (-> game (load-deck :corp) (load-deck :runner))
        corp-deck (get-in game [:corp :deck])
        runner-deck (get-in game [:runner :deck])]
    (is (not= corp-deck (shuffle-deck game :corp)))
    (is (not= runner-deck (shuffle-deck game :runner)))
    (is (= 46 (count-deck runner-deck)))))

(facts "starting game without decks")
(comment
  (is (nil? (start-game game))))

(facts "starting game")
(facts "drawing cards")

(defn draw-card [game side]
  (-> game
      (update-in [side :hand] concat (peek-at-top-card game side))
      (update-in [side :deck :cards] rest)))

(defn draw-cards [game side n]
  (if (zero? n)
    game
    (draw-cards (draw-card game side) side (- n 1))))

(defn discard-card [game side card] game)
(defn discard-hand [game side] game)

(defn shuffle-discard-into-deck [game side] game)

(defn mulligan [game side]
  (-> game
      (discard-hand side)
      (shuffle-discard-into-deck side)
      (draw-cards side 5)
      (assoc-in [:corp :mulligan] true)))

(defn tag-runner [game]
  (update-in game [:runner :tags] inc))

(defn remove-tag [game]
  (let [tags (get-in game [:runner :tags])]
    (if (pos? tags)
      (update-in game [:runner :tags] dec))))

(facts "tagging"
       (tag-runner {:runner {:tags 0}}) => {:runner {:tags 1}}
       (remove-tag {:runner {:tags 1}}) => {:runner {:tags 0}}
       (remove-tag {:runner {:tags 0}}) => nil)

(defn bad-pub-corp [game]
  (update-in game [:corp :bad-pubs] inc))

(defn remove-bad-pub [game]
  (let [bad-pubs (get-in game [:corp :bad-pubs])]
    (if (pos? bad-pubs)
      (update-in game [:corp :bad-pubs] dec))))

(facts "bad pubs"
       (bad-pub-corp {:corp {:bad-pubs 0}}) => {:corp {:bad-pubs 1}}
       (remove-bad-pub {:corp {:bad-pubs 1}}) => {:corp {:bad-pubs 0}}
       (remove-bad-pub {:corp {:bad-pubs 0}}) => nil)

(defn play [game card])

(defn click-for-credit [game side])
(defn click-for-card [game side])
(defn click-for-resource-trash [game id])
(defn click-for-tag-removal [game])
