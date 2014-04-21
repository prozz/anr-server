(ns netrunner.deck-test
  (:use clojure.test
        clojure.repl
        netrunner.cards
        netrunner.deck))

(deftest validating
    (testing "cards props"
             (let [deck (parse-deck (slurp "resources/ct.deck" :encoding "UTF-8"))
                   deck-id (get-identity deck)
                   femme-id (title->id "femme fatale")]
               (is (= "Chaos Theory: Wünderkind" (get-title deck-id)))
               (is (= "shaper" (get-faction deck-id)))
               (is (= 40 (get-minimum-decksize deck-id)))
               (is (= 15 (get-influence-limit deck-id)))
               (is (same-faction? deck-id deck-id))
               (is (not (same-faction? deck-id femme-id)))
               (is (= 1 (get-faction-influence femme-id)))))
    (testing "valid deck"
             (let [deck (parse-deck (slurp "resources/ct.deck" :encoding "UTF-8"))]
               (is (valid-deck? deck))
               (is (= 40 (count-deck deck)))
               (is (= 15 (count-influence deck)))))
    (testing "invalid deck"
             (let [deck (parse-deck (slurp "resources/ct-invalid.deck" :encoding "UTF-8"))]
               (is (not (valid-deck? deck)))
               (is (= 42 (count-deck deck)))
               (is (= 21 (count-influence deck))))))

(comment
  (run-tests 'netrunner.deck-test)
  (run-all-tests #"netrunner.*-test")
)
