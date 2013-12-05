(ns netrunner.cards-test
  (:use clojure.test
        clojure.repl
        netrunner.cards
        netrunner.deck))

(deftest validating
  (let [db (generate-db)]
    (testing "cards props"
             (let [deck (parse-deck db (slurp "resources/ct.deck" :encoding "UTF-8"))
                   deck-id (get-identity deck)
                   femme-id (title->id db "femme fatale")]
               (is (= "Chaos Theory: Wünderkind" (get-title db deck-id)))
               (is (= "shaper" (get-faction db deck-id)))
               (is (= 40 (get-minimum-decksize db deck-id)))
               (is (= 15 (get-influence-limit db deck-id)))
               (is (same-faction? db deck-id deck-id))
               (is (not (same-faction? db deck-id femme-id)))
               (is (= 1 (get-faction-influence db femme-id)))))
    (testing "valid deck"
             (let [deck (parse-deck db (slurp "resources/ct.deck" :encoding "UTF-8"))]
               (is (valid-deck? db deck))))
    (testing "invalid deck"
             (let [deck (parse-deck db (slurp "resources/ct-invalid.deck" :encoding "UTF-8"))]
               (is (not (valid-deck? db deck)))))))
             
(comment
  (run-tests 'netrunner.cards-test)
)


