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
       (draw-card {:corp {:deck {:cards '()} :hand '()}} :corp) => {:corp {:deck {:cards '()} :hand '()}}
       (draw-card {:corp {:deck {:cards '()} :hand '(1 2 3)}} :corp) => {:corp {:deck {:cards '()} :hand '(1 2 3)}}
       (draw-cards {:corp {:deck {:cards '(1 2 3)} :hand '()}} :corp 2) => {:corp {:deck {:cards '(3)} :hand '(1 2)}}
       (draw-cards {:corp {:deck {:cards '(1 2 3)} :hand '()}} :corp 5) => {:corp {:deck {:cards '()} :hand '(1 2 3)}}
       (draw-cards {:corp {:deck {:cards '()} :hand '()}} :corp 3) => {:corp {:deck {:cards '()} :hand '()}}
       (draw-cards {:corp {:deck {:cards '()} :hand '(1 2 3)}} :corp 3) => {:corp {:deck {:cards '()} :hand '(1 2 3)}})


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
