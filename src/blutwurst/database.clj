(ns blutwurst.database 
  (:import (java.sql DriverManager JDBCType)))

(defn read-table-row [rs] { 
                          :name (.getString rs "TABLE_NAME") 
                          :schema (.getString rs "TABLE_SCHEM")
                          })


(defn build-table-list [rs result] 
                          (if (not (.next rs))
                            result
                            (recur rs (cons (read-table-row rs) result))
                          ))

(defn retrieve-tables [spec]
 (let [connection (DriverManager/getConnection (:connection-string spec))
       meta-data (.getMetaData connection)
       result-set (.getTables meta-data nil nil nil (into-array ["TABLE"]))
       
       result (build-table-list result-set [])]

   (.close connection)
   result
 ))

(defn read-columns [rs result]
  (if (not (.next rs))
    result
    (recur rs (cons { 
                     :name (.getString rs "COLUMN_NAME") 
                     :type (.toString (JDBCType/valueOf (.getInt rs "DATA_TYPE")) )
                     :length (.getInt rs "COLUMN_SIZE")
                    } 
                    result))
    ))

(defn retrieve-columns-for-table [meta-data table]
                             (let [rs (.getColumns meta-data 
                                                   nil 
                                                   (:schema table) 
                                                   (:name table) 
                                                   nil)]
                              (read-columns rs [])
                               ))

(defn retrieve-columns [spec tables]
  (let [connection (DriverManager/getConnection (:connection-string spec))
        meta-data (.getMetaData connection)]
    (map (fn [table] 
           (assoc table :columns (retrieve-columns-for-table meta-data table)))
         tables)
  ))

(defn retrieve-table-graph [spec]
 "This function accepts a connection specification and produces a table graph."
 (let [retrieve-columns (partial retrieve-columns spec)]
     { 
      :tables (-> spec
                  retrieve-tables
                  retrieve-columns)
     }))
