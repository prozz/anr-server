(ns netrunner.game
  (:use [netrunner.cards :refer :all]))

(defn game-ready? [game]
  (and 
    (contains? game :corp_deck)
    (contains? game :runner_deck)))

(defn start-game [game]
  "if both decks are loaded prepares all needed game related entities:
   - hq/grip, 
   - archives/heap,
   - clicks
   - credits
   - tags
   - bad pubs"
  (if (game-ready? game)
    (merge game
           {:corp_state {:hq [], :archives [], :clicks 3, :credits 5, :bad-pubs 0}}
           {:runner_state {:grip [], :heap [], :clicks 4, :credits 5, :tags 0}})))

