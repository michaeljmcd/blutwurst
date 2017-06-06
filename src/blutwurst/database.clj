(ns blutwurst.database
    (:import (java.sql DriverManager)))

(defrecord schema-graph [tables dependencies])

(defn retrieve-tables [spec]
 (let [connection (DriverManager/getConnection (:connection-string spec))
       meta-data (.getMetaData connection)
       result-set (.getTables meta-data nil nil nil (into-array ["TABLE"]))]
   (while (.next result-set)
     (println "on row")
     (println (.getString result-set "TABLE_NAME"))
     )
 ))

(defn retrieve-table-graph [spec]
 "This function accepts a connection specification and produces a table graph."
 (let [tables (retrieve-tables spec)]
   nil
   ))
