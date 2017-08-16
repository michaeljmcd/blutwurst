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
  )
 ))

 ; TODO: make test useful
(deftest integration-tests
 (testing "End to end flow."
  (with-out-str
    (core/-main "app.jar" "-c" connection-string "-f" "csv" "-o" "-" "-v"))
  ))
