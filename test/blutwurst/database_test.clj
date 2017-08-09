(ns blutwurst.database-test
  (:require [clojure.test :refer :all]
            [blutwurst.database :refer :all]
            [blutwurst.logging-fixture :refer :all]
            [blutwurst.in-memory-db-utilities :refer :all]
            [clojure.data :refer :all]
            [clojure.pprint :refer :all]))

(use-fixtures :each db-fixture logging-fixture)

(deftest table-graph-tests
  (testing "Connects to an in-memory database and returns basic table list."
     (let [spec {:connection-string connection-string}
           table-graph (retrieve-table-graph spec)
           expected {:tables
                       [{:name "PURCHASE",
                         :schema "DBO",
                         :dependencies {
                            ["PURCHASEDBYID"] {:name "PERSON" :schema "DBO" :columns ["ID"]}
                         }
                         :columns
                         [{:name "PURCHASEDBYID",
                           :type "INTEGER",
                           :length 10,
                           :nullable false}
                          {:name "AMOUNT", :type "DECIMAL", :length 65535, :nullable true}
                          {:name "ID", :type "INTEGER", :length 10, :nullable true}]}
                        {:name "PERSON",
                         :schema "DBO",
                         :dependencies []
                         :columns
                         [{:name "NAME", :type "VARCHAR", :length 100, :nullable true}
                          {:name "ID", :type "INTEGER", :length 10, :nullable true}]}]
                       }]
       (is (= expected table-graph)))
   ))
