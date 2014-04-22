(ns netrunner.util)

(defn now [] (java.util.Date.))

(defn rotate-forward [xs]
  "moves first item in list to last position, ex: (1 2 3 4) -> (2 3 4 1)"
  (concat (rest xs) [(first xs)]))

(defn rotate-backward [xs]
  "moves last item in list to first position, ex: (1 2 3 4) -> (4 1 2 3)"
  (cons (last xs) (drop-last xs)))

(comment
  (rotate-forward '(1 2 3 4))
  (rotate-backward '(1 2 3 4))
  )
