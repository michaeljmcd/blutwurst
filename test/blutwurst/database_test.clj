(ns blutwurst.database-test
  (:require [clojure.test :refer :all]
            [blutwurst.database :refer :all]
            [blutwurst.in-memory-db-utilities :refer :all]
            [clojure.data :refer :all]
            [clojure.pprint :refer :all]))

(use-fixtures :each db-fixture)

(deftest table-graph-tests
  (testing "Connects to an in-memory database and returns basic table list."
     (let [spec {:connection-string connection-string}
           table-graph (retrieve-table-graph spec)
           expected {:tables
                       '({:name "PURCHASE",
                         :schema "DBO",
                         :columns
                         ({:name "PURCHASEDBYID",
                           :type "INTEGER",
                           :length 10,
                           :nullable false}
                          {:name "AMOUNT", :type "DECIMAL", :length 65535, :nullable true}
                          {:name "ID", :type "INTEGER", :length 10, :nullable true})}
                        {:name "PERSON",
                         :schema "DBO",
                         :columns
                         ({:name "NAME", :type "VARCHAR", :length 100, :nullable true}
                          {:name "ID", :type "INTEGER", :length 10, :nullable true})})}]
       ;(pprint (diff expected table-graph))
       (is (= expected table-graph)))
   ))
