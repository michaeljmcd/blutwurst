(ns blutwurst.in-memory-db-utilities
  (:import (java.sql DriverManager)))

(defn clear-database [connection-string]
  (let [connection (DriverManager/getConnection connection-string)
        statement (.createStatement connection)]
    (.execute statement "drop all objects")
    (.close statement)))

(defn create-test-tables [connection-string]
  (let [connection (DriverManager/getConnection connection-string)
        statement (.createStatement connection)
        table-creation-sql ["create schema dbo"
                            "create table dbo.PurchaseType (Name varchar(50), Category varchar(50))"
                            "create table dbo.Person (ID int, Name varchar(100), BirthDate date)"
                            "create table dbo.Purchase (ID int, Amount decimal(5,2), 
                                PurchaseTypeName varchar(50),
                                PurchaseTypeCategory varchar(50),
                                PurchasedByID int not null, 
                                -- foreign key (PurchaseTypeName, PurchaseTypeCategory) references dbo.PurchaseType(Name, Category),
                                -- foreign key (PurchasedByID) references dbo.Person(ID)
                            )"]]
    (doseq [i table-creation-sql]
      (.execute statement i))
    (.close statement)))

(def connection-string "jdbc:h2:mem:test")

(defn db-fixture [f]
  (create-test-tables connection-string)
  (f)
  (clear-database connection-string))

