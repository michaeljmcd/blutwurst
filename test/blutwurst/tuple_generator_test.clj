(ns blutwurst.tuple-generator-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer :all]
            [taoensso.timbre :as timbre :refer [trace]]
            [blutwurst.logging-fixture :refer :all]
            [blutwurst.tuple-generator :refer :all]))

(def fixed-generators
  ^{ :private true }
  [
   {
      :name "Random String Generator"
      :determiner #(= (:type %) "VARCHAR")
      :generator (fn [x] "asdf")
   }
   {
       :name "Random Integer Generator"
       :determiner #(= (:type %) "INTEGER")
       :generator (fn [c] 100)
   }
   {
       :name "Random Decimal Generator"
       :determiner #(= (:type %) "DECIMAL")
       :generator (fn [c] 1.7)
   }
  ])

(defn predictable-generator-fixture [f]
 (with-redefs [blutwurst.tuple-generator/value-generation-strategies fixed-generators]
  (f)
 ))

(use-fixtures :each logging-fixture predictable-generator-fixture)

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
                      }]
      (is (= {:table table-spec
              :tuples [{:Address1 "asdf" :City "asdf"} {:Address1 "asdf" :City "asdf"}]} 
             (generate-tuples-for-table table-spec 2)))
    )))

(deftest generate-tuples-from-plan-test   
    (testing "Multiple data types."
        (let [table-spec '({
                          :name "Destination" 
                          :schema "foo" 
                          :columns ({:name "Address1" :type "VARCHAR" :length 20 :nullable false}
                                     {:name "ID" :type "INTEGER" :length 3 :nullable true})
                          })
              expected-tuples (repeat 100 {:Address1 "asdf" :ID 100})]
        (is (= `({:table ~(first table-spec)
                 :tuples ~expected-tuples
                 })
                (generate-tuples-for-plan table-spec)
               ))
      )))
; TODO: build a negative test with an unknown data type
