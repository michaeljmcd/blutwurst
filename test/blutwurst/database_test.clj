(ns blutwurst.database-test
  (:require [clojure.test :refer :all]
            [blutwurst.database :refer :all]
            [blutwurst.logging-fixture :refer :all]
            [blutwurst.in-memory-db-utilities :refer :all]
            [clojure.data :refer :all]
            [clojure.pprint :refer :all]))

(use-fixtures :each db-fixture logging-fixture)

(def person-table {:name "PERSON",
                         :schema "DBO",
                         :dependencies []
                         :columns
                         [{:name "NAME", :type "VARCHAR", :length 100, :nullable true}
                          {:name "ID", :type "INTEGER", :length 10, :nullable true}]})

(def full-expected-graph {:tables
                       [{:name "PURCHASETYPE" :schema "DBO" 
                         :columns [
                          {:name "CATEGORY" :type "VARCHAR" :length 50 :nullable true}
                          {:name "NAME" :type "VARCHAR" :length 50 :nullable true}
                         ]
                         :dependencies [] }
                        {:name "PURCHASE",
                         :schema "DBO",
                         :dependencies [
                            { 
                              :dependency-name "CONSTRAINT_96"
                              :target-name "PERSON"
                              :target-schema "DBO"
                              :links {
                                "PURCHASEDBYID" "ID"
                              }
                            }
                            { 
                              :dependency-name "CONSTRAINT_9"
                              :target-name "PURCHASETYPE"
                              :target-schema "DBO"
                              :links {
                                "PURCHASETYPENAME" "NAME"
                                "PURCHASETYPECATEGORY" "CATEGORY"
                              }
                            }
                         ]
                         :columns
                         [{:name "PURCHASEDBYID",
                           :type "INTEGER",
                           :length 10,
                           :nullable false}
                          {:name "PURCHASETYPECATEGORY" :type "VARCHAR" :length 50 :nullable true}
                          {:name "PURCHASETYPENAME" :type "VARCHAR" :length 50 :nullable true}
                          {:name "AMOUNT", :type "DECIMAL", :length 65535, :nullable true}
                          {:name "ID", :type "INTEGER", :length 10, :nullable true}]}
                          person-table
                        ]
                       })

(deftest table-graph-tests
  (testing "Returns only specified tables when a list is provided."
    (let [spec {:connection-string connection-string :included-tables '("PERSON")}
          table-graph (retrieve-table-graph spec)]
      (is (= {:tables [person-table]} table-graph))
    ))

  (testing "Connects to an in-memory database and returns basic table list."
     (let [spec {:connection-string connection-string}
           table-graph (retrieve-table-graph spec)]
       (is (= full-expected-graph table-graph))))
   
   (testing "Table scans exclude schemas when at least one is provided."
     (let [spec {:connection-string connection-string :included-schemas '("FOOBAR")}
           table-graph (retrieve-table-graph spec)]
       (is (= {:tables []} table-graph))))

   (testing "Table scans include schemas when at least one is provided."
     (let [spec {:connection-string connection-string :included-schemas '("DBO")}
           table-graph (retrieve-table-graph spec)]
       (is (= full-expected-graph table-graph)))
   ))
