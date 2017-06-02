(ns blutwurst.database
    (:import (java.sql DriverManager)))

(defrecord schema-graph [tables dependencies])
(defrecord database-spec [connection-string])

(defn retrieve-table-graph [spec]
 "This function accepts a connection specification and produces a table graph."
 (let [connection (DriverManager/getConnection (:connection-string spec))
       meta-data (.getMetaData connection)]
 nil
 ))
