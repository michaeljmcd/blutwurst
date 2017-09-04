(ns blutwurst.tuple-formatter
  (:import (java.util Date) (java.text SimpleDateFormat))
  (:require [clojure.data.csv :as csv]
            [clojure.core.strint :refer [<<]]
            [cheshire.core :as json]
            [taoensso.timbre :as timbre :refer [trace]]))

(defn- extract-data-from-row [row]
  (->> row
       (mapv #(if (map? (second %))
                (extract-data-from-row (second %))
                (second %)))
       flatten))

(defn- extract-data-from-table-tuples [table]
  (let [rows (:tuples table)]
    (mapv extract-data-from-row rows)))

(defn- extract-column-names [prefix row]
  (flatten (map  #(if (map? (second %))
                    (extract-column-names (if (nil? prefix)
                                            (name (first %))
                                            (str prefix "." (name (first %))))
                                          (second %))
                    (if (not (empty? prefix))
                      (str prefix "." (-> % first name))
                      (-> % first name)))
                 row)))

(defn- create-csv-column-list [table]
  (->> table
       :tuples
       first
       (extract-column-names nil)
       vector))

(defn- csv-formatter [spec table]
  (let [column-list (create-csv-column-list table)]
    {:entity (:entity table)
     :tuples (vector (let [writer (new java.io.StringWriter)]
                       (csv/write-csv writer
                                      (concat column-list
                                              (extract-data-from-table-tuples table)))
                       (.toString writer)))}))

(defn- comma-delimit [values]
  (reduce (fn [a, b] (if (empty? (str a)) b (str a "," b))) values))

(defn- sql-identifier [token]
  (str "\"" token "\""))

(defn- make-table-name [table]
  (keyword (str (-> table :entity :schema)
                "."
                (-> table :entity :name))))

(defn- parenthesize [v] (str "(" v ")"))

(defn- build-columns [table]
  (->> table
       :tuples
       first
       (extract-column-names nil)
       (mapv (fn [column] (sql-identifier column)))
       comma-delimit
       parenthesize))

(defn- sql-value-string [values]
  (map #(if (string? %)
          (str "'" (clojure.string/replace % "'" "''") "'")
          %)
       values))

(defn- format-date [date]
  (let [simple-format (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")]
    (.format simple-format date)))

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
         (extract-data-from-table-tuples table))))

(defn- sql-formatter
  "This will generate SQL-1999 insert statements for the attached table and return it as a string."
  [spec table]
  (let [schema (if (not (empty? (-> table :entity :schema)))
                 (str (sql-identifier (-> table :entity :schema)) ".")
                 "")
        table-name (-> table :entity :name)
        columns (build-columns table)
        tuples (build-tuples table)]

    {:entity (:entity table)
     :tuples (vector (<< "INSERT INTO ~{schema}\"~{table-name}\" ~{columns} VALUES ~{tuples};\n"))}))

(defn- json-formatter [spec table]
  {:entity (:entity table)
   :tuples (vector (json/generate-string (:tuples table)))})

(defn format-rows [spec tables]
  (mapv (partial (case (:format spec)
                   :csv csv-formatter
                   :json json-formatter
                   :sql sql-formatter)
                 spec)
        tables))
