(ns blutwurst.tuple-formatter
    (:import (java.util Date) (java.text SimpleDateFormat))
    (:require [clojure.data.csv :as csv]
              [clojure.core.strint :refer [<<]]
              [cheshire.core :as json]
              [taoensso.timbre :as timbre :refer [trace]]))

(defn- extract-data-from-table-tuples [table]
 (let [rows (:tuples table)]
    (mapv (fn [r] (mapv #(second %) r)) rows)
 ))

(defn- csv-formatter [spec table]
   {:table (:table table)
    :tuples (vector (with-out-str 
                       (csv/write-csv *out* 
                                      (concat (vector (map #(:name %) (:columns (:table table))))
                                              (extract-data-from-table-tuples table)))
                       ))
   })

(defn- comma-delimit [values]
 (reduce (fn [a, b] (if (empty? (str a)) b (str a "," b))) values))

(defn- sql-identifier [token]
 (str "\"" token "\""))

(defn- make-table-name [table]
    (keyword (str (-> table :table :schema) 
                  "." 
                  (-> table :table :name))))

(defn- parenthesize [v] (str "(" v ")"))

(defn- build-columns [table]
 (->> (-> table :table :columns)
      (mapv (fn [column] (sql-identifier (:name column))))
      comma-delimit
      parenthesize
 ))

(defn- sql-value-string [values]
 (map #(if (string? %)
        (str "'" (clojure.string/replace % "'" "''") "'")
        %)
      values))

(defn- format-date [date]
 (let [simple-format (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")]
  (.format simple-format date)
 ))

(defn- sql-date [values]
 (map #(if (instance? Date %)
        (str "'" (format-date %) "'")
        %)
      values))

(defn- build-tuples [table]
 (comma-delimit
     (mapv (fn [tuple] (->> tuple 
                            sql-value-string 
                            sql-date 
                            comma-delimit 
                            parenthesize))
           (extract-data-from-table-tuples table))
 ))

(defn- sql-formatter 
  "This will generate SQL-1999 insert statements for the attached table and return it as a string."
  [spec table]
  (let [schema (if (not (empty? (-> table :table :schema)))
                  (str (sql-identifier (-> table :table :schema)) ".")
                  "")
        table-name (-> table :table :name)
        columns (build-columns table)
        tuples (build-tuples table)]

   { 
    :table (:table table)
    :tuples (vector (<< "INSERT INTO ~{schema}\"~{table-name}\" ~{columns} VALUES ~{tuples};\n"))
   }
))

(defn- json-formatter [spec table] 
  {
   :table (:table table) 
   :tuples (vector (json/generate-string (:tuples table)))
  })

(defn format-rows [spec tables]
 (mapv (partial (case (:format spec)
                  :csv csv-formatter
                  :json json-formatter
                  :sql sql-formatter) 
                spec)
       tables))
