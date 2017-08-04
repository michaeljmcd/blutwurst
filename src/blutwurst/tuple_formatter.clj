(ns blutwurst.tuple-formatter
    (:require [clojure.data.csv :as csv]
              [honeysql.format :as sql]
              [taoensso.timbre :as timbre :refer [trace]]))

(defn- extract-data-from-table-tuples [data]
 (let [rows (:tuples data)]
    (mapv (fn [r] (mapv #(second %) r)) rows)
 ))

(defn- csv-formatter [spec data]
   {:table (:table data)
    :tuples (with-out-str (csv/write-csv *out* (extract-data-from-table-tuples data))) })

(defn- make-table-name [data]
    (keyword (str (-> data :table :schema) "." (-> data :table :name))))

(defn- sql-formatter 
  "This will generate SQL-1999 insert statements for the attached data and return it as a string."
  [spec table]
  (let [statement {:insert-into [(make-table-name table) (mapv #(-> % :name keyword) (-> table :table :columns))]
                       :values (extract-data-from-table-tuples table)}]
    (trace statement)
   (first (sql/format statement :quoting :ansi))
  ))

(defn format-rows [spec tables]
 (map (partial (case (:format spec)
         :csv csv-formatter
         :sql sql-formatter) spec)
       tables))
