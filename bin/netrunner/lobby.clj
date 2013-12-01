(ns netrunner.lobby
  (:use [clojure.repl]
        [clojure.set]
        [netrunner.util]))

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

(defn opposite-side [side]
  ({:corp :runner :runner :corp} side))

(defn owner-side [game]
  (if (= (:owner game) (:corp game)) :corp :runner))

(defn create-game [games username side]
  (let [id (next-game-id)]
    (assoc games id {:id id :owner username side username :created (now)})))

(defn awaiting? [game]
  (not (contains? game :opp)))

(defn ongoing? [game]
  (not (awaiting? game)))

(defn owner? [game username]
  (= username (:owner game)))
  
;; unjoin and destroy better?
(defn remove-game [games id]
  (dissoc games id))

(defn join-game [games id username]
  (if (contains? games id)
    (let [game (games id)
          side (opposite-side (owner-side game))]
    (assoc games id (merge game {:opp username side username :started (now)})))))

(defn list-games 
  ([games] (filter awaiting? (vals games)))
  ([games side] (filter #(and (awaiting? %) (contains? % side)) (vals games))))
