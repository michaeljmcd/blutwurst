(ns blutwurst.sink-test
  (:require [clojure.test :refer :all]
            [blutwurst.sink :refer :all]))

(deftest sink-creation-test
 (testing "Creating a new sink for stdout based on a specification."
  (let [sink (make-sink {:output-file "-"})]
   (is (= blutwurst.sink/standard-output-sink sink))
 ))

 (testing "Creates a file sink for non-stdout options."
    (with-redefs-fn {#'blutwurst.sink/make-file-sink (fn [x] "asdf")}
      #(is (= "asdf" (blutwurst.sink/make-sink {:output-file "a.txt"})))
     ))

  (testing "Fallback to the null sink if nothing is provided."
   (let [sink (make-sink {})]
    (is (= blutwurst.sink/null-sink sink)))
  ))
