(ns blutwurst.jsonschema-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer :all]
            [clojure.java.io :as io]
            [blutwurst.logging-fixture :refer :all]
            [blutwurst.jsonschema :refer :all]))

(use-fixtures :each logging-fixture)

(deftest json-schema-parsing
  (testing "Basic JSON Schema parsing tests."
    (let [spec {:connection-string (io/resource "address.json")}
          expected {:entities [{:name "UNKNOWN1" :schema nil :dependencies [] :type :complex
                              :properties [{:name "post-office-box" :type :string :constraints {:nullable true}}
                                        {:name "extended-address" :type :string :constraints {:nullable true}}
                                        {:name "street-address" :type :string :constraints {:nullable true}}
                                        {:name "locality" :type :string :constraints {:nullable false}}
                                        {:name "region" :type :string :constraints {:nullable false}}
                                        {:name "postal-code" :type :string :constraints {:nullable true}}
                                        {:name "country-name" :type :string :constraints {:nullable false}}]}]}
          result (parse-json-schema-from-spec spec)]
      (is (= expected result))))

  (testing "Handles simple arrays."
   (let [spec {:connection-string (io/resource "arrays.json")}
         result (parse-json-schema-from-spec spec)]
     (is (= {:entities [{:name "Arrays" :schema nil :type :complex :dependencies []
                       :properties [{:name "id" :type :integer :constraints {:nullable true}}
                                 {:name "tags" :type :sequence :constraints {:nullable true} :properties [{:type :string :dependencies [] :schema nil :name "items" :properties []}]}]
                                 }]} result))
   )
  )

#_(testing "Handles complex arrays."
       (let [spec {:connection-string (io/resource "arrays2.json")}
             result (parse-json-schema-from-spec spec)]
         (is (= {:tables [{:name "Arrays" :schema nil :dependencies [{:target-schema nil :target-name "UNKNOWN1" :dependency-name nil :links {"tags" :embedded}}]
                           :columns [{:name "id" :type "INTEGER" :nullable true}
                                     {:name "tag-sets" :type "OBJECT" :nullable true :container "array"}]
                                     },
                          {:name "UNKNOWN1" :schema nil :dependencies []
                           :columns [{:name "foo" :type "STRING" :nullable true}
                                     {:name "baz" :type "DECIMAL" :nullable true :container "array"}]}]} result))
       )
      )

(testing "Handles 2-ply objects."
    (let [spec {:connection-string (io/resource "hero.json")}
          expected  {:entities '({:dependencies []
                       :name "Hero"
                       :properties [{:constraints {:nullable true}
                                     :name "name"
                                     :type :string}
                                    {:constraints {:nullable true}
                                     :dependencies []
                                     :name "weapon"
                                     :properties [{:constraints {:nullable true}
                                                   :name "type"
                                                   :type :string}
                                                  {:constraints {:nullable true}
                                                   :name "range"
                                                   :type :decimal}]
                                     :schema nil
                                     :type :complex}]
                       :schema nil
                       :type :complex})}
          result (parse-json-schema-from-spec spec)]
      (is (= expected result)))))
