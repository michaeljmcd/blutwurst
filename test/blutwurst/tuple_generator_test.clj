(ns blutwurst.tuple-generator-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer :all]
            [blutwurst.tuple-generator :refer :all]))

(deftest tuple-generator-test
  (testing "Basic tuple generator."
    (let [table-spec {
                      :name "Destination" 
                      :schema "foo" 
                      :columns '({:name "Address1" :type "VARCHAR" :length 20 :nullable false}
                                 {:name "City" :type "VARCHAR" :length 100 :nullable true})
                      }
          data (generate-tuples-for-table table-spec 5)]
      (pprint data)
      (is (= 5 (count data)))
    )))
