(ns blutwurst.database-test
  (:require [clojure.test :refer :all]
            [blutwurst.database :refer :all]))

(deftest table-graph-tests
  (testing "Connects to an in-memory database and returns null."
    (is (= 0 1))
    ))
