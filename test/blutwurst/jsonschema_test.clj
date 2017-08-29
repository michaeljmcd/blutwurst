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
          expected {:tables [{:name "UNKNOWN1" :schema nil :dependencies []
                              :columns [{:name "post-office-box" :type "STRING" :nullable true}
                                        {:name "extended-address" :type "STRING" :nullable true}
                                        {:name "street-address" :type "STRING" :nullable true}
                                        {:name "locality" :type "STRING" :nullable false}
                                        {:name "region" :type "STRING" :nullable false}
                                        {:name "postal-code" :type "STRING" :nullable true}
                                        {:name "country-name" :type "STRING" :nullable false}]}]}
          result (parse-json-schema-from-spec spec)]
      (is (= expected result))))

  (testing "Handles 2-ply objects."
    (let [spec {:connection-string (io/resource "hero.json")}
          expected {:tables [{:name "Hero" :schema nil
                              :dependencies [{:target-schema nil :target-name "weapon" :dependency-name nil :links {"weapon" :embedded}}]
                              :columns [{:name "name" :type "STRING" :nullable true}
                                        {:name "weapon" :type "OBJECT" :nullable true}]}
                             {:name "weapon" :schema nil :dependencies []
                              :columns [{:name "type" :type "STRING" :nullable true}
                                        {:name "range" :type "DECIMAL" :nullable true}]}]}
          result (parse-json-schema-from-spec spec)]
      (is (= expected result)))))
