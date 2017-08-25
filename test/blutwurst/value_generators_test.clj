(ns blutwurst.value-generators-test
  (:import (java.lang Math))
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer :all]
            [blutwurst.value-generators :as vg]))

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

(deftest random-value-test
 (testing "Basic random number generator respects size of smallint."
  (let [generator-fn (->> vg/value-generation-strategies
                         (filter #(= "Integer Generator" (:name %)))
                         first
                         :generator)]
    (dotimes [iter 100]
     (let [value (generator-fn {:name "foo" :type "TINYINT"})]
       (is (and (<= value 255)
                (>= value 0))
           "SMALLINT must be in the range of values for one byte.")
    ))

    ; TODO: these should all allow for negative values.
    (dotimes [iter 100]
     (let [value (generator-fn {:name "foo" :type "SMALLINT"})]
       (is (and (<= value (- (Math/pow 2 15) 1))
                (>= value 0))
           "INTEGER must be in the range of values for 2 bytes.")
    ))

    (dotimes [iter 100]
     (let [value (generator-fn {:name "foo" :type "BIGINT"})]
       (is (and (<= value (- (Math/pow 2 63) 1))
                (>= value 0))
           "INTEGER must be in the range of values for 8 bytes.")
    ))

    (dotimes [iter 100]
     (let [value (generator-fn {:name "foo" :type "INTEGER"})]
       (is (and (<= value (- (Math/pow 2 32) 1))
                (>= value 0))
           "INTEGER must be in the range of values for 4 bytes.")
    ))
   ))

 (testing "Basic random string generation."
   (let [value (vg/random-string 10)]
    (is (< (count value) 10)))
 ))

(deftest generator-list-test
 (testing "Module returns a valid list of known generators."
   (with-redefs-fn {#'vg/create-generators #(do % fixed-generators)}
    #(let [expected (list "Random String Generator" "Random Integer Generator" "Random Decimal Generator")]
     (is (= expected (vg/retrieve-registered-generators nil)))
   ))
 ))

(deftest generator-creation-test
  (testing "Generators passed through."
    (with-redefs [vg/value-generation-strategies fixed-generators]
     (let [result (vg/create-generators {:format :csv})]
      (is (= 3 (count result)))
    ))
  )

  (testing "Regex generators are added."
    (with-redefs [vg/value-generation-strategies fixed-generators]
     (let [result (vg/create-generators {:regex-generators [{:name "asdf" :regex "asdf"}]})]
      (is (= 4 (count result)))
    ))
  ))

