(ns blutwurst.core-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer :all]
            [clojure.tools.trace :as trace]
            [clojure.tools.cli :refer [parse-opts]]
            [blutwurst.core :as core]
            [blutwurst.logging-fixture :refer :all]
            [blutwurst.in-memory-db-utilities :refer :all]))

(use-fixtures :each db-fixture logging-fixture)

(deftest option-parsing-tests
 (testing "Schema options accumulate."
  (let [args (list "test.jar" "-s" "S1" "-s" "S2")
        result (parse-opts args core/cli-options)]
    (is (= (list "S2" "S1") (-> result :options :schema)))
    (is (= (list "S2" "S1") (:included-schemas (core/build-spec (:options result)))))
  ))

  (testing "Parses options for row count."
    (let [args (list "test.jar" "-n" "30")
          result (parse-opts args core/cli-options)]
      (is (= 30 (-> result :options :number-of-rows)))
      (is (= 30 (:number-of-rows (core/build-spec (:options result)))))
    ))

  (testing "Parses option to list generators."
   (let [args (list "test.jar" "--list-generators")
         result (parse-opts args core/cli-options)]
    (is (= true (-> result :options :list-generators)))
  ))

  (testing "List generators is not the default."
    (let [args (list "test.jar" "--list-gens")
             result (parse-opts args core/cli-options)]
        (is (= nil (-> result :options :list-generators)))
      )))

 ; TODO: make test useful
(deftest integration-tests
 (testing "End to end flow."
  (with-out-str
    (core/-main "app.jar" "-c" connection-string "-f" "csv" "-o" "-" "-v"))
  ))
