(ns blutwurst.tuple-formatter-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer :all]
            [blutwurst.tuple-formatter :refer :all]))

(deftest csv-formatter-test
  (testing "Generating a CSV from rows."
    (let [spec {:format :csv :output "-"}
          rows [[["a" 1] ["b" 2]]]]
        (format-rows spec rows)
      )
    ))
