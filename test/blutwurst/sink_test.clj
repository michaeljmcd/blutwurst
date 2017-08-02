(ns blutwurst.sink-test
  (:require [clojure.test :refer :all]
            [blutwurst.sink :refer :all]))

(deftest sink-creation-test
 (testing "Creating a new sink for stdout based on a specification."
  (let [sink (make-sink {:output "-"})]
   (is (= blutwurst.sink/standard-output-sink sink))
 ))
  (testing "Fallback to the standard out sink of nothing is provided."
   (let [sink (make-sink {})]
    (is (= blutwurst.sink/null-sink sink)))
  ))
