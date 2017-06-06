(ns blutwurst.database-test
  (:require [clojure.test :refer :all]
            [blutwurst.database :refer :all])
    (:import (java.sql DriverManager)))

(def connection-string "jdbc:h2:mem:test")

(defn create-test-tables [connection-string] 
  (let [connection (DriverManager/getConnection connection-string)
        statement (.createStatement connection)
        table-creation-sql ["create schema dbo" "create table dbo.Person (ID int, Name varchar(100))"]]
    (doseq [i table-creation-sql]
      (.execute statement i))
    (.close statement)
  ))

(deftest table-graph-tests
  (testing "Connects to an in-memory database and returns basic table list."
   (let [spec {:connection-string connection-string}]
       (create-test-tables connection-string)
     (let [table-graph (retrieve-table-graph spec)]
       (println table-graph)
       (is (not (= table-graph nil)))
       ))
   ))
