(ns netrunner.game-test
  (:use clojure.test
        clojure.repl
        clojure.tools.trace
        netrunner.game
        netrunner.deck
        netrunner.cards
        netrunner.lobby
        ))

(defn prepare-game []
  (let [games (create-game {} "john" :corp)
        id (first (keys games))]
    (first (vals (join-game games id "barry")))))
    
(deftest playing-game
         (let [game (prepare-game)]
           (testing "deck loading"
                    (is (false? (contains? game :corp_deck)))
                    (is (false? (contains? game :runner_deck)))
                    (is (true? (contains? (load-deck game :corp) :corp_deck)))
                    (is (true? (contains? (load-deck game :runner) :runner_deck))))
           (testing "deck shuffling"
                    (let [game (-> game (load-deck :corp) (load-deck :runner))
                      corp-deck (:corp_deck game)
                      runner-deck (:runner_deck game)]
                      (is (not= corp-deck (shuffle-deck game :corp)))
                      (is (not= runner-deck (shuffle-deck game :runner)))
                      (is (= 46 (count-deck runner-deck)))))
           (testing "starting game without decks"
                    (is (nil? (start-game game))))
           (testing "starting game"
                    (let [game (-> game 
                                 (load-deck :corp) 
                                 (load-deck :runner)
                                 (start-game))]
                      (println game)))
           (testing "drawing cards"
                    (let [game (-> game 
                                 (load-deck :corp) 
                                 (load-deck :runner)
                                 (start-game))
                          game (trace (draw-card game :runner))
                          x (println game)]
                    (is (= 45 (count-deck game :runner)))
                    ))))


(comment
  (run-tests 'netrunner.game-test)
  (run-all-tests #"netrunner.*-test")
)

