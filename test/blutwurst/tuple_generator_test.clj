(ns blutwurst.tuple-generator-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer :all]
            [taoensso.timbre :as timbre :refer [trace]]
            [blutwurst.logging-fixture :refer :all]
            [blutwurst.value-generators :as vg]
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

(use-fixtures :each logging-fixture)

(deftest generate-tuples-from-plan-test   
    (testing "Multiple data types."
    (with-redefs-fn {#'vg/create-generators #(do % fixed-generators)}
      #(let [table-spec '({
                          :name "Destination" 
                          :schema "foo" 
                          :columns ({:name "Address1" :type "VARCHAR" :length 20 :nullable false}
                                     {:name "ID" :type "INTEGER" :length 3 :nullable true})
                          })
             spec {:number-of-rows 10}]

        (is (= `({:table ~(first table-spec)
                 :tuples ~(repeat 10 {:Address1 "asdf" :ID 100})
                 })
                (generate-tuples-for-plan spec table-spec)
               ))
      ))))

(deftest generate-tuples-with-foreign-keys
    (testing "Foreign key values are all found in source table."
      (let [weapon-table '{:name "Weapon" :schema "asdf" :columns ({:name "ID" :type "INTEGER" :length 3 :nullable false}
                                                                    {:name "Name" :type "VARCHAR" :length 3 :nullable false})}
            hero-table '{:name "Hero" :schema "asdf" :columns ({:name "Name" :type "VARCHAR" :length 200}
                                                                {:name "PrimaryWeaponID" :type "INTEGER" :length 3})
                         :dependencies [
                           {:target-name "Weapon" :target-schema "asdf" :links {"PrimaryWeaponID" "ID"}}
                         ]
                        }
            spec {:number-of-rows 100}
            result (generate-tuples-for-plan spec (list weapon-table hero-table))
            generated-weapons (-> result first :tuples)]

        (is (reduce (fn [a b] (and a b)) 
                    (map #(some (fn [c] (= (:ID c) (:PrimaryWeaponID %))) generated-weapons) 
                         (-> result second :tuples))))
     )))
; TODO: build a negative test with an unknown data type

(deftest generator-overrides
 (testing "Generator override is used."
   (with-redefs-fn {#'vg/create-generators #(do % fixed-generators)}
     #(let [spec {:column-generator-overrides (list {:column-pattern "^ID$" :generator-name "Random Decimal Generator"})
                :number-of-rows 5}
          tables '({
                          :name "Destination"
                          :schema "foo"
                          :columns ({:name "Address1" :type "VARCHAR" :length 20 :nullable false}
                                     {:name "ID" :type "INTEGER" :length 3 :nullable true})
                          })]
      (is (= `({:table ~(first tables) :tuples ~(repeat 5 {:Address1 "asdf" :ID 1.7})})
              (generate-tuples-for-plan spec tables)))
    ))))
