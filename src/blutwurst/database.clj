(ns blutwurst.database 
  (:require [taoensso.timbre :as timbre :refer [trace]]
            [clojure.string :as cstring]
            [clojure.tools.reader.edn :as edn]
            [clojure.java.io :as io])
  (:import (java.sql DriverManager JDBCType)
           (java.lang Class)))

(def ^:private 
     jdbc-drivers (-> "drivers.txt" 
                       io/resource 
                       slurp 
                       edn/read-string))

(defn- find-class-for-spec [connection-string]
     (trace "Looking up driver class for connection string " connection-string)
     (trace "Driver list: " jdbc-drivers)

     (let [driver-entries (filter #(cstring/starts-with? connection-string (first %)) jdbc-drivers)]
       (trace "Found drivers: " (pr-str (seq driver-entries)))
       (:classname (second (first driver-entries)))
      ))

(def ^:private load-driver-for-connection-string (memoize (fn [connection-string]
  (.newInstance (Class/forName (find-class-for-spec connection-string))))
 ))

(defmacro with-jdbc-meta-data 
 "Accepts a specification object and a function accepting a single argument (metadata).
  This macro will establish a database connection, retrieve the meta data object, invoke 
  the function on it and cleanup afterwards."
 [spec fun]
 (let [connection-name (gensym)
       meta-data-name (gensym)
       result-name (gensym)]
   `(do
       (load-driver-for-connection-string (:connection-string ~spec))

       (let [~connection-name (DriverManager/getConnection (:connection-string ~spec))
             ~meta-data-name (.getMetaData ~connection-name)
             ~result-name (apply ~fun (list ~meta-data-name))]
            ~result-name))
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
   #(cond 
      (not (empty? (:included-tables spec)))
        (mapcat (fn [s] (build-table-list (.getTables % nil nil s (into-array ["TABLE"])) [])) 
                        (:included-tables spec))
      (not (empty? (:included-schemas spec)))
        (mapcat (fn [s] (build-table-list (.getTables % nil s nil (into-array ["TABLE"])) [])) 
                        (:included-schemas spec))
      :else
        (build-table-list (.getTables % nil nil nil (into-array ["TABLE"])) [])

     )))

(defn- string->boolean [input]
  (case input
    "YES" true
    "NO" false
    nil))

(defn- read-columns [rs result]
  (if (not (.next rs))
    result
    (do
      (trace "Found column " (.getString rs "COLUMN_NAME") " with type " (.getString rs "TYPE_NAME"))

      (recur rs (cons { 
                       :name (.getString rs "COLUMN_NAME") 
                       :type (.getString rs "TYPE_NAME")
                       :length (.getInt rs "COLUMN_SIZE")
                       :nullable (string->boolean (.getString rs "IS_NULLABLE"))
                      } 
                      result)))
    ))

(defn- retrieve-columns-for-table [meta-data table]
 (trace "Retrieving columns for " table)

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
 "This function exists to find foreign keys between tables and return them for inclusion in the table object.
  The main reason that this is included in the table structure and not outside of it is that it will be
  needed when doing foreign key subselects."
  [spec tables] 
 (with-jdbc-meta-data spec
   #(mapv (fn [table] (assoc table :dependencies (retrieve-dependencies-for-table % table)))
          tables)
  ))

(defn retrieve-table-graph [spec]
 "This function accepts a connection specification and produces a table graph."
 (let [table-list (->> spec
                       retrieve-tables
                       (retrieve-columns spec)
                       (retrieve-keys spec))]
     { 
      :tables table-list
     }))
