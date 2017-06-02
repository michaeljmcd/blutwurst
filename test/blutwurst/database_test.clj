(ns blutwurst.database-test
  (:require [clojure.test :refer :all]
            [blutwurst.database :refer :all])
    (:import (java.sql DriverManager)))

(defn create-test-tables []
 (let [connection (DriverManager/getConnection "jdbc:h2:mem:")
       statement (.createStatement connection)
       table-creation-sql ["create schema dbo" "create table dbo.Person (ID int, Name varchar(100))"]]
    (for [i table-creation-sql]  (.execute statement i))
 ))

(deftest table-graph-tests
  (testing "Connects to an in-memory database and returns null."
   (let [spec (->database-spec "jdbc:h2:mem:")]
       (create-test-tables)
       (let [table-graph (retrieve-table-graph spec)]
    (is (not (= table-graph nil)))
   )
    )))
