(ns blutwurst.tuple-generator-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer :all]
            [blutwurst.tuple-generator :refer :all]))

(deftest random-string-test
 (testing "Basic random string generation."
   (let [value (random-string 10)]
    (is (< (count value) 10)))
 ))

(deftest tuple-generator-test
  (testing "Basic tuple generator."
    (let [table-spec {
                      :name "Destination" 
                      :schema "foo" 
                      :columns '({:name "Address1" :type "VARCHAR" :length 20 :nullable false}
                                 {:name "City" :type "VARCHAR" :length 100 :nullable true})
                      }
          data (generate-tuples-for-table table-spec 5)]
      (is (= 2 (count data)))
    )))

(deftest generate-tuples-from-plan-test   
(testing "Multiple data types."
    (let [table-spec '({
                      :name "Destination" 
                      :schema "foo" 
                      :columns '({:name "Address1" :type "VARCHAR" :length 20 :nullable false}
                                 {:name "ID" :type "INTEGER" :length 3 :nullable true})
                      })
          data (generate-tuples-for-plan table-spec)]
      (is (= 1 (count data)))
      (is (= 2 (count (first data))))
  )))
; TODO: build a negative test with an unknown data type
