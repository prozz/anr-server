(ns netrunner.util
  (:use [midje.sweet]))

; TODO consider going clj-time completely
(defn now [] (java.util.Date.))

(defn rotate-forward [xs]
  "moves first item in list to last position, ex: (1 2 3 4) -> (2 3 4 1)"
  (concat (rest xs) [(first xs)]))

(fact "rotates forward"
      (rotate-forward '(1 2 3 4)) => '(2 3 4 1))

(defn rotate-backward [xs]
  "moves last item in list to first position, ex: (1 2 3 4) -> (4 1 2 3)"
  (cons (last xs) (drop-last xs)))

(fact "rotates backward"
      (rotate-backward '(1 2 3 4)) => '(4 1 2 3))

(defn deep-merge [& maps]
  (apply merge-with deep-merge maps))

(fact "deep-merge combines nested maps without overwrites"
      (deep-merge {:runner {:deck {}}} {:runner {:clicks 4}}) => {:runner {:deck {} :clicks 4}})

(defn remove-first [x seq]
  (let [[n m] (split-with (partial not= x) seq)]
    (concat n (rest m))))

(fact "remove-first get rids of an item in a list that may contain it's repetitions"
      (remove-first 3 '()) => '()
      (remove-first 1 '(1 2 3 1)) => '(2 3 1)
      (remove-first 2 '(1 1 2 2 3)) => '(1 1 2 3))
