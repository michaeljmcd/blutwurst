(ns blutwurst.database 
  (:import (java.sql DriverManager)))

(defn retrieve-tables [spec]
 (let [read-table-row (fn [rs] { 
                                :name (.getString rs "TABLE_NAME") 
                                :schema (.getString rs "TABLE_SCHEM")
                                })

       build-table-list (fn [rs result] 
                          (if (not (.next rs))
                            result
                            (recur rs (cons (read-table-row rs) result))
                          ))

       connection (DriverManager/getConnection (:connection-string spec))
       meta-data (.getMetaData connection)
       result-set (.getTables meta-data nil nil nil (into-array ["TABLE"]))
       
       result (build-table-list result-set [])]

   (.close connection)
   result
 ))

(defn retrieve-table-graph [spec]
 "This function accepts a connection specification and produces a table graph."
 (let [tables (retrieve-tables spec)]
   { :tables tables }
   ))
