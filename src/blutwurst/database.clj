(ns blutwurst.database 
  (:require [taoensso.timbre :as timbre :refer [trace]])
  (:import (java.sql DriverManager JDBCType)))

(defmacro with-jdbc-meta-data 
 "Accepts a specification object and a function accepting a single argument (metadata).
  This macro will establish a database connection, retrieve the meta data object, invoke 
  the function on it and cleanup afterwards."
 [spec fun]
 (let [connection-name (gensym)
       meta-data-name (gensym)
       result-name (gensym)]
   `(let [~connection-name (DriverManager/getConnection (:connection-string ~spec))
          ~meta-data-name (.getMetaData ~connection-name)
          ~result-name (apply ~fun (list ~meta-data-name))]
      ~result-name)
 ))

(defn- read-table-row [rs] { 
                           :name (.getString rs "TABLE_NAME") 
                           :schema (.getString rs "TABLE_SCHEM")
                           })

(defn- build-table-list [rs result] 
                          (if (not (.next rs))
                            result
                            (recur rs (cons (read-table-row rs) result))
                          ))

(defn- retrieve-tables [spec]
 (with-jdbc-meta-data spec
    #(build-table-list (.getTables % nil nil nil (into-array ["TABLE"])) [])
))

(defn- string->boolean [input]
  (case input
    "YES" true
    "NO" false
    nil))

(defn- read-columns [rs result]
  (if (not (.next rs))
    result
    (recur rs (cons { 
                     :name (.getString rs "COLUMN_NAME") 
                     :type (.toString (JDBCType/valueOf (.getInt rs "DATA_TYPE")) )
                     :length (.getInt rs "COLUMN_SIZE")
                     :nullable (string->boolean (.getString rs "IS_NULLABLE"))
                    } 
                    result))
    ))

(defn- retrieve-columns-for-table [meta-data table]
 (let [rs (.getColumns meta-data 
                       nil 
                       (:schema table) 
                       (:name table) 
                       nil)]
  (read-columns rs [])
   ))

(defn- retrieve-columns [spec tables]
 (with-jdbc-meta-data spec
     #(mapv (fn [table] 
                (assoc table :columns (retrieve-columns-for-table % table)))
            tables)))

; This code is gnarly. Need to find a better way to represent what is going on
; here. Basically, we are creating a hierarchical structure from a flat one in a
; recursive function.

(defn- read-dependencies [rs last-key target-table target-schema links result]
 (if (not (.next rs))
  (if (empty? links)
   result
   (conj result {:dependency-name last-key :target-name target-table :target-schema target-schema :links links}))

  (let [key-name (.getString rs "FK_NAME")
        current-column (.getString rs "FKCOLUMN_NAME")
        current-to-column (.getString rs "PKCOLUMN_NAME")]
    (recur rs
           key-name
           (.getString rs "PKTABLE_NAME")
           (.getString rs "PKTABLE_SCHEM")
           (if (or (= key-name last-key)
                   (nil? last-key))
            (assoc links current-column current-to-column)
            {current-column current-to-column})
           (if (or (= key-name last-key)
                   (nil? last-key))
            result
            (conj result {:dependency-name last-key :target-name target-table :target-schema target-schema :links links}))
     ))
 ))

(defn- retrieve-dependencies-for-table [meta-data table]
  (read-dependencies (.getImportedKeys meta-data nil (:schema table) (:name table))
    nil
    nil
    nil
    {}
    []))

(defn- retrieve-keys 
 "This function is responsible for building an adjacency matrix illustrating the key relationships
 between tables."
 [spec tables] 
 (with-jdbc-meta-data spec
   #(mapv (fn [table] (assoc table :dependencies (retrieve-dependencies-for-table % table)))
          tables)))

(defn retrieve-table-graph [spec]
 "This function accepts a connection specification and produces a table graph."
 (let [table-list (->> spec
                       retrieve-tables
                       (retrieve-columns spec)
                       (retrieve-keys spec))]
     { 
      :tables table-list
     }))
