(ns blutwurst.jsonschema
 (:require [cheshire.core :as json]
           [clojure.java.io :as io]
           [clojure.string :as string]
           [clojure.pprint :refer :all]
           [taoensso.timbre :as timbre :refer [trace]]))

(defn- lookup-type [prop]
 (let [raw-value (second (first (second prop)))]
  (case (string/upper-case raw-value )
   "STRING" "STRING"
   "NUMBER" "DECIMAL"
   "INTEGER" "INTEGER"
   "OBJECT" "OBJECT"
  )))

(defn- extract-properties [schema res] 
 (assoc res :columns
     (mapv (fn [prop] {:name (first prop) :type (lookup-type prop)})
           (get schema "properties"))))

(def ^:private unknown-count (atom 0))

(defn- create-name-for-entity []
 (swap! unknown-count inc)
 (str "UNKNOWN" @unknown-count))

(defn- extract-basics [schema]
 {:schema nil :name (or (get schema "title") (create-name-for-entity)) :dependencies []}) 

(defn- determine-nullability-for-column [schema column]
 (let [required-columns (set (get schema "required"))]
   (assoc column :nullable (not (contains? required-columns (:name column))))))

(defn- determine-nullabilty [schema res] 
 (assoc res :columns (mapv (partial determine-nullability-for-column schema) (:columns res))))

(defn- extract-dependencies [schema res] 
 res)

(defn- find-child-schemas [schema]
 (let [aggregate-properties (filter #(= "object" (get (second %) "type")) (get schema "properties"))]
  (map #(if (nil? (get (second %) "title"))
          (assoc (second %) "title" (first %))
          (second %)
             ) aggregate-properties)
 )
)

(defn- map-schema [schema-list result]
 (if (empty? schema-list)
  result
(let [schema (first schema-list)
 mapped-schema 
  (->> schema
       extract-basics
       (extract-properties schema)
       (determine-nullabilty schema)
       (extract-dependencies schema))

       child-schemas (find-child-schemas schema)]
  (recur (concat (rest schema-list) child-schemas)
   (conj result mapped-schema)))
))

(defn parse-json-schema 
 "Accepts location, which should be coercible to a reader via clojure.java.io.reader,
  and returns an object formatted like a schema description used throughout Blutwurst."
 [location] 
 (with-open [r (io/reader location)]
  (let [json-document (json/parse-stream r)]
   {:tables (map-schema (list json-document) [])})))

(defn parse-json-schema-from-spec [spec] 
 (-> spec
     :connection-string
     parse-json-schema))
