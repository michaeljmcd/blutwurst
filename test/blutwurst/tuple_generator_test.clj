(ns blutwurst.tuple-generator-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer :all]
            [taoensso.timbre :as timbre :refer [trace]]
            [blutwurst.logging-fixture :refer :all]
            [blutwurst.value-generators :as vg]
            [blutwurst.tuple-generator :refer :all]))

(def fixed-generators
  ^{:private true}
  [{:name "Random String Generator"
    :determiner #(= (:type %) :string)
    :generator (fn [x] "asdf")}
   {:name "Random Integer Generator"
    :determiner #(= (:type %) :integer)
    :generator (fn [c] 100)}
   {:name "Random Decimal Generator"
    :determiner #(= (:type %) :decimal)
    :generator (fn [c] 1.7)}])

(use-fixtures :each logging-fixture)

(deftest generate-tuples-from-plan-test
  (testing "Multiple data types."
    (with-redefs-fn {#'vg/create-generators #(do % fixed-generators)}
      #(let [table-spec '({:name "Destination"
                           :schema "foo"
                           :type :complex
                           :properties ({:name "Address1" :type :string :constraints {:maximum-length 20 :nullable false}}
                                        {:name "ID" :type :integer :constraints {:maximum-length 3 :nullable true}})})
             spec {:number-of-rows 10}]

         (is (= `({:entity ~(first table-spec)
                   :tuples ~(repeat 10 {:Address1 "asdf" :ID 100})})
                (generate-tuples-for-plan spec table-spec)))))))

(deftest generate-tuples-with-foreign-keys
  (testing "Passing an unknown type."
    (let [a-table '{:name "ASDF" :type :complex :schema "foo" :properties ({:name "BAZ" :type :ixian})}
          result (generate-tuples-for-plan {} (list a-table))]
      (is (thrown? NullPointerException (pr-str (seq result))))))

  (testing "Embeds full objects for that kind of dependency."
    (with-redefs-fn {#'vg/create-generators #(do % fixed-generators)}
      #(let [person-table {:name "Person" :type :complex :schema nil :properties [{:name "Address" :schema nil :properties [{:name "Address1" :type :string :constraints {:maximum-length 10}}
                                                                                                             {:name "City" :type :string :constraints {:maximum-length 10}}] :type :complex} {:name "Name" :type :string}]
                           :dependencies []}
             spec {:number-of-rows 2}
             result (generate-tuples-for-plan spec (list person-table))]
         (is (= `({:entity ~person-table :tuples ~(repeat 2 {:Name "asdf" :Address {:Address1 "asdf" :City "asdf"}})})
                result)))))

  (testing "Foreign key values are all found in source table."
    (let [weapon-table '{:name "Weapon" :type :complex :schema "asdf" :properties ({:name "ID" :type :integer :constraints {:maximum-value 255 :nullable false}}
                                                                    {:name "Name" :type :string :constraints {:maximum-length 3 :nullable false}})}
          hero-table '{:name "Hero" :type :complex :schema "asdf" :properties ({:name "Name" :type :string :constraints {:maximum-length 200}}
                                                                {:name "PrimaryWeaponID" :type :integer :constraints {:maximum-value 255}})
                       :dependencies [{:target-name "Weapon" :target-schema "asdf" :links {"PrimaryWeaponID" "ID"}}]}
          spec {:number-of-rows 100}
          result (generate-tuples-for-plan spec (list weapon-table hero-table))
          generated-weapons (-> result first :tuples)]

      (is (reduce (fn [a b] (and a b))
                  (map #(some (fn [c] (= (:ID c) (:PrimaryWeaponID %))) generated-weapons)
                       (-> result second :tuples)))))))

(deftest generator-overrides
  (testing "Ignores columns that are passed in the ignore list."
    (with-redefs-fn {#'vg/create-generators #(do % fixed-generators)}
      #(let [weapon-table '{:name "Weapon" :type :complex :schema "asdf" :properties ({:name "ID" :type :integer :constraints {:maximum-length 3 :nullable false}}
                                                                       {:name "Name" :type :string :constraints {:maximum-length 3 :nullable false}})}
             spec {:number-of-rows 2 :ignored-columns ["I.*"]}]
         (is (= `({:entity ~weapon-table :tuples ~(repeat 2 {:Name "asdf"})})
                (generate-tuples-for-plan spec (list weapon-table)))))))

  (testing "Ignores columns that are passed in the ignore list when they are also foreign keys."
    (with-redefs-fn {#'vg/create-generators #(do % fixed-generators)}
      #(let [weapon-table '{:name "Weapon" :type :complex :schema "asdf" :properties ({:name "ID" :type :integer :constraints {:maximum-length 3 :nullable false}}
                                                                       {:name "Name" :type :string :constraints {:maximum-length 3 :nullable false}})}
             hero-table '{:name "Hero" :type :complex :schema "asdf" :properties ({:name "Name" :type :string :constraints {:maximum-length 200}}
                                                                   {:name "PrimaryWeaponID" :type :integer :constraints {:maximum-length 3}})
                          :dependencies [{:target-name "Weapon" :target-schema "asdf" :links {"PrimaryWeaponID" "ID"}}]}
             spec {:number-of-rows 1 :ignored-columns ["PrimaryWeaponID"]}
             result (generate-tuples-for-plan spec (list weapon-table hero-table))]
         (is (= (list {:Name "asdf"}) (:tuples (nth result 1)))))))

  (testing "Generator override is used."
    (with-redefs-fn {#'vg/create-generators #(do % fixed-generators)}
      #(let [spec {:column-generator-overrides (list {:column-pattern "^ID$" :generator-name "Random Decimal Generator"})
                   :number-of-rows 5}
             tables '({:name "Destination"
                       :schema "foo" :type :complex
                       :properties ({:name "Address1" :type :string :constraints {:maximum-length 20 :nullable false}}
                                    {:name "ID" :type :integer :constraints {:maximum-length 3 :nullable true}})})]
         (is (= `({:entity ~(first tables) :tuples ~(repeat 5 {:Address1 "asdf" :ID 1.7})})
                (generate-tuples-for-plan spec tables)))))))

(deftest simple-generation
 (testing "Generates simple string values."
  (with-redefs-fn {#'vg/create-generators #(do % fixed-generators)}
  #(let [table {:name "foo" :type :string}
         result (generate-tuples-for-plan {:number-of-rows 2} (list table))]
   (is (= `({:entity ~table :tuples ~(repeat 2 {:foo "asdf"})}) result))
  ))
 )
)

(deftest complex-generation
 (testing "Generates a list of primitve values."
  (with-redefs-fn {#'vg/create-generators #(do % fixed-generators)}
    #(let [spec {:format :csv :number-of-rows 2}
           plan '({:name "foo"  :type :complex :schema nil :properties ({:name "nums" :type :sequence :properties ({:name "items" :type "string"})})})
           result (generate-tuples-for-plan spec plan)]
(is (= `({:entity ~(first plan) :tuples ~(repeat 2 {:nums [1 2 3]})})
           result))
     )
  )))
