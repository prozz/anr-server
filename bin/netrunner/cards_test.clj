(ns netrunner.cards-test
  (:use clojure.test
        clojure.repl
        netrunner.cards))

(deftest searching
  (let [db (generate-db)]
    (testing "existing cards written oddly"
             (is (= 1050 (title->id db "sure gamble")))
             (is (= 1050 (title->id db "Sure gaMble")))
             (is (= 1050 (title->id db "sure  gamble")))
             (is (= 1050 (title->id db " sure gamble "))))
    (testing "no such card"
             (is (nil? (title->id db "notexistingcardname"))))))

(comment
  (run-tests 'netrunner.cards-test)
)


