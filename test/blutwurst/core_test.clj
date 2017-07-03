(ns blutwurst.core-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer :all]
            [clojure.tools.trace :as trace]
            [blutwurst.core :as core]
            [blutwurst.in-memory-db-utilities :refer :all]))

(use-fixtures :each db-fixture)

(deftest main-tests
  (testing "Basic test of the main function's workflow."
    (with-redefs [core/build-spec (fn [a] a)]
      (is (= '() (core/-main "app.jar")
             ))
    )))

(deftest integration-tests
 (testing "End to end flow."
  (is (= 2 (count (core/-main "app.jar" "-c" connection-string))))
 ))
