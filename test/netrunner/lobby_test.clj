(ns netrunner.lobby-test
  (:use clojure.test
        clojure.repl
        netrunner.lobby))

(deftest utils
  (is (= :corp (opposite-side :runner)))
  (is (= :runner (opposite-side :corp)))
  (is (= :corp (owner-side {:owner "john" :corp "john"})))
  (is (= :runner (owner-side {:owner "john" :runner "john"}))))

(deftest games-validator
  (testing "all ok"
    (is (validate-games {1 {:opp "john"}, 2 {:owner "barry"}}))
    (is (validate-games {1 {:opp "john"}, 2 {:opp "barry"}})))
  (testing "duplicates"
    (is (not (validate-games {1 {:opp "john"}, 2 {:opp "john"}})))
    (is (not (validate-games {1 {:owner "john"}, 2 {:owner "john"}})))
    (is (not (validate-games {1 {:opp "john"}, 2 {:owner "john"}})))))

(deftest game-creation
  (testing "new game" 
    (let [games (create-game {} "john" :corp)]
      (is (= 1 (count games)))
      (is (= (first (keys games)) (:id (first (vals games)))))))
  (testing "new game map"
    (let [game (first (vals (create-game {} "john" :corp)))]
      (is (every? game [:id :owner :created]))
      (is (awaiting? game))))
  (testing "new game for corp side"
    (let [game (first (vals (create-game {} "john" :corp)))]
      (is (= "john" (:corp game)))))
  (testing "new game for runner side"
    (let [game (first (vals (create-game {} "john" :runner)))]
      (is (= "john" (:runner game))))))
           
(deftest game-joining
  (testing "joining not existing game"
    (is (nil? (join-game {} 1 "john"))))
  (testing "joining existing game"
    (let [games (create-game {} "john" :corp)
          id (first (keys games))]
      (is (not (nil? (join-game games id "barry"))))))
  (testing "map after join"
    (let [games (create-game {} "john" :corp)
          id (first (keys games))
          game (first (vals (join-game games id "barry")))]
      (is (every? game [:id :owner :created :opp :started :runner :corp])))))

(comment
  (run-tests 'netrunner.lobby-test)
  (run-all-tests #"netrunner.*-test")
)


