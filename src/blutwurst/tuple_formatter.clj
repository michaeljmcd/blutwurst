(ns blutwurst.tuple-formatter
    (:require [clojure.data.csv :as csv]
              [clojure.core.strint :refer [<<]]
              [taoensso.timbre :as timbre :refer [trace]]))

(defn- extract-data-from-table-tuples [table]
 (let [rows (:tuples table)]
    (mapv (fn [r] (mapv #(second %) r)) rows)
 ))

(defn- csv-formatter [spec table]
   {:table (:table table)
    :tuples (with-out-str (csv/write-csv *out* (extract-data-from-table-tuples table))) })

(defn- make-table-name [table]
    (keyword (str (-> table :table :schema) "." (-> table :table :name))))

(defn- sql-formatter 
  "This will generate SQL-1999 insert statements for the attached table and return it as a string."
  [spec table]
  (let [schema (-> table :table :schema)
        table-name (-> table :table :name)]
   (<< "INSERT INTO ~{schema}\"~{table-name}\"")))

(defn format-rows [spec tables]
 (map (partial (case (:format spec)
         :csv csv-formatter
         :sql sql-formatter) spec)
       tables))
