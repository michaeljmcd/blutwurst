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

  (testing "Handles simple arrays."
   (let [spec {:connection-string (io/resource "arrays.json")}
         result (parse-json-schema-from-spec spec)]
     (is (= {:tables [{:name "Arrays" :schema nil :dependencies []
                       :columns [{:name "id" :type "INTEGER" :nullable true}
                                 {:name "tags" :type "STRING" :nullable true :container "array"}]
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
          expected {:tables [{:name "Hero" :schema nil
                              :dependencies [{:target-schema nil :target-name "weapon" :dependency-name nil :links {"weapon" :embedded}}]
                              :columns [{:name "name" :type "STRING" :nullable true}
                                        {:name "weapon" :type "OBJECT" :nullable true}]}
                             {:name "weapon" :schema nil :dependencies []
                              :columns [{:name "type" :type "STRING" :nullable true}
                                        {:name "range" :type "DECIMAL" :nullable true}]}]}
          result (parse-json-schema-from-spec spec)]
      (is (= expected result)))))
