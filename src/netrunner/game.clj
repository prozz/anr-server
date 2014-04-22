(ns netrunner.game
  (:use [netrunner.cards :refer :all]
        [netrunner.deck :refer :all]))

(defn game-ready? [game]
  (and
    (contains? game :corp_deck)
    (contains? game :runner_deck)))

(defn hand-key [side]
  ({:runner :grip :corp :hq} side))

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
      (merge {:hq '(), :archives '(), :corp_clicks 3, :corp_credits 5, :bad-pubs 0}
             {:grip '(), :heap '(), :runner_clicks 4, :runner_credits 5, :tags 0})
      (shuffle-deck :runner)
      (shuffle-deck :corp))))

(def testing-game (-> {} (load-deck :corp) (load-deck :runner) (start-game)))

(defn draw-card [game side]
  (-> game
      (update-in [(hand-key side)] concat (peek-at-top-card game side))
      (update-in [(deck-key side) :cards] rest)))

(defn draw-cards [game side n]
  (if (zero? n)
    game
    (draw-cards (draw-card game side) side (- n 1))))
