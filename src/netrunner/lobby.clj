(ns netrunner.lobby
  (:use [clojure.repl]
        [clojure.set]
        [netrunner.util]
        [midje.sweet]))

(def game-id (atom 0))
(defn next-game-id [] (swap! game-id inc))

(defn validate-games [games]
  "no username should appear twice"
  (let [owners (map :owner (vals games))
        opps (map :opp (vals games))
        usernames (filter (comp not nil?) (concat owners opps))]
    (or (empty? usernames)
        (apply distinct? usernames))))

(defn init [] (atom {} :validator validate-games))

(facts "games validation"
       (facts "all ok"
              (fact (validate-games {1 {:opp "john"}, 2 {:owner "barry"}}) => true)
              (fact (validate-games {1 {:opp "john"}, 2 {:opp "barry"}}) => true))
       (facts "duplicates"
              (fact (validate-games {1 {:opp "john"}, 2 {:opp "john"}}) => false)
              (fact (validate-games {1 {:owner "john"}, 2 {:owner "john"}}) => false)
              (fact (validate-games {1 {:opp "john"}, 2 {:owner "john"}}) => false)))

(defn opposite-side [side]
  ({:corp :runner :runner :corp} side))

(facts (opposite-side :runner) => :corp
       (opposite-side :corp) => :runner)

(defn owner-side [game]
  (if (= (:owner game) (:corp game)) :corp :runner))

(facts (owner-side {:owner "john" :corp "john"}) => :corp
       (owner-side {:owner "john" :runner "john"}) => :runner)

(defn create-game [games username side]
  (let [id (next-game-id)]
    (assoc games id {:id id :owner username side username :created (now)})))

(defn awaiting? [game]
  (not (contains? game :opp)))

(defn ongoing? [game]
  (not (awaiting? game)))

(defn owner? [game username]
  (= username (:owner game)))

(facts "game creation"
       (let [games (create-game {} "john" :corp)
             game (first (vals games))]
         (fact "single game" (count games) => 1)
         (fact "ids are same" (first (keys games)) => (:id (first (vals games))))
         (fact "new game is awaiting" (awaiting? game) => true)
         (fact "new game has owner" (:owner game) =not=> nil)
         (fact "new game owner" (owner? game "john") => true)
         (fact "new game has id and created date" (every? game [:id :created]) => true)
         (fact "new game corp is john" (:corp game) => "john")
         (fact "new game has no runner" (:runner game) => nil))
       (let [games (create-game {} "john" :runner)
             game (first (vals games))]
         (fact "new game runner is john" (:runner game) => "john")
         (fact "new game has no corp" (:corp game) => nil)))

;; unjoin and destroy better?
(defn remove-game [games id]
  (dissoc games id))

(defn join-game [games id username]
  (if (contains? games id)
    (let [game (games id)
          side (opposite-side (owner-side game))]
      (assoc games id (merge game {:opp username side username :started (now)})))))

(facts "game joining"
       (fact "cannot join not existing game" (join-game {} 1 "john") => nil)
       (let [games (create-game {} "john" :corp)
             game-id (first (keys games))
             games-after-join (join-game games game-id "barry")
             game-with-barry (first (vals games-after-join))]
         (fact "can join exisitng game" games-after-join =not=> nil)
         (fact "ids match" game-id => (:id game-with-barry))
         (fact "all game properties are in place" (every? game-with-barry [:id :owner :created :opp :started :runner :corp]) => true)))


(defn list-games
  ([games] (filter awaiting? (vals games)))
  ([games side] (filter #(and (awaiting? %) (contains? % side)) (vals games))))
