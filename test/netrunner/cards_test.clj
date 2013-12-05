(ns netrunner.cards-test
  (:use clojure.test
        clojure.repl
        netrunner.cards))

(deftest searching
    (testing "existing cards written oddly"
             (is (= 1050 (title->id "sure gamble")))
             (is (= 1050 (title->id "Sure gaMble")))
             (is (= 1050 (title->id "sure  gamble")))
             (is (= 1050 (title->id " sure gamble "))))
    (testing "no such card"
             (is (nil? (title->id "notexistingcardname")))))

(comment
  (run-tests 'netrunner.cards-test)
  (run-all-tests #"netrunner.*-test")
)


