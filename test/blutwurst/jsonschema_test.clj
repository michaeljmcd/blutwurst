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
                                       {:name "tags" :type :sequence :constraints {:nullable true} :properties [{:type :string :dependencies [] :schema nil :name "items" :properties []}]}]}]} result))))

  (testing "Handles complex arrays."
    (let [spec {:connection-string (io/resource "arrays2.json")}
          result (parse-json-schema-from-spec spec)]
      (is (=  {:entities '({:dependencies []
                            :name "Arrays"
                            :properties [{:constraints {:nullable true}
                                          :name "id"
                                          :type :integer}
                                         {:constraints {:nullable true}
                                          :name "tag-sets"
                                          :properties [{:dependencies []
                                                        :name "items"
                                                        :properties [{:dependencies []
                                                                      :name "UNKNOWN2"
                                                                      :properties [{:constraints {:nullable true}
                                                                                    :name "foo"
                                                                                    :type :string}
                                                                                   {:constraints {:nullable true}
                                                                                    :name "baz"
                                                                                    :properties [{:dependencies []
                                                                                                  :name "items"
                                                                                                  :properties []
                                                                                                  :schema nil
                                                                                                  :type :decimal}]
                                                                                    :type :sequence}]
                                                                      :schema nil
                                                                      :type :complex}]
                                                        :schema nil
                                                        :type :sequence}]
                                          :type :sequence}]
                            :schema nil
                            :type :complex})} result))))

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
