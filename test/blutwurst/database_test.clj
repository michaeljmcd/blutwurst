(ns blutwurst.database-test
  (:require [clojure.test :refer :all]
            [blutwurst.database :refer :all]
            [clojure.data :refer :all])
    (:import (java.sql DriverManager)))

(def connection-string "jdbc:h2:mem:test")

(defn clear-database [connection-string]
  (let [connection (DriverManager/getConnection connection-string)
        statement (.createStatement connection)]
    (.execute statement "drop all objects")
    (.close statement)
  ))

(defn create-test-tables [connection-string] 
  (let [connection (DriverManager/getConnection connection-string)
        statement (.createStatement connection)
        table-creation-sql [
                            "create schema dbo" 
                            "create table dbo.Person (ID int, Name varchar(100))"
                            "create table dbo.Purchase (ID int, Amount decimal, PurchasedByID int not null, foreign key (PurchasedByID) references dbo.Person(ID))"
                            ]]
    (doseq [i table-creation-sql]
      (.execute statement i))
    (.close statement)
  ))

(defn db-fixture [f]
  (create-test-tables connection-string)
  (f)
  (clear-database connection-string))

(use-fixtures :each db-fixture)

(deftest table-graph-tests
  (testing "Connects to an in-memory database and returns basic table list."
     (let [spec {:connection-string connection-string}
           table-graph (retrieve-table-graph spec)
           expected {:tables '({:name "PURCHASE", :schema "DBO", :columns ({:name
                                      "PURCHASEDBYID", :type "INTEGER", :length
                                      10 :nullable false} {:name "AMOUNT", :type "DECIMAL",
                                      :length 65535 :nullable true} {:name "ID", :type
                                      "INTEGER", :length 10 :nullable true})} {:name "PERSON",
                              :schema "DBO", :columns ({:name "NAME", :type
                                      "VARCHAR", :length 100 :nullable true} {:name "ID", :type
                                      "INTEGER", :length 10 :nullable true})})}]
       (println table-graph)
       ;(println (diff expected table-graph))

       (is (= expected table-graph)))
   ))
