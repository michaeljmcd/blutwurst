(ns blutwurst.in-memory-db-utilities
    (:import (java.sql DriverManager)))

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
