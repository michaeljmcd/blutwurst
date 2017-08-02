(ns blutwurst.tuple-formatter-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer :all]
            [blutwurst.tuple-formatter :refer :all]))

(deftest csv-formatter-test
  (testing "Generating a CSV from rows."
    (let [spec {:format :csv :output "-"}
          rows '(((("A" 1) ("B" 2))))]
        (format-rows spec rows)
      )))
