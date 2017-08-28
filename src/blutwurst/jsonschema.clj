(ns blutwurst.jsonschema
 (:require [cheshire.core :as json]
           [clojure.java.io :as io]
           [clojure.pprint :refer :all]
           [taoensso.timbre :as timbre :refer [trace]]))

(defn- extract-properties [schema res] 
 (pprint schema)
 (assoc res :columns
     (mapv (fn [prop] {:name (first prop) :type (second (first (second prop)))})
           (get schema "properties"))))

(defn- extract-basics [schema]
 {:schema (get schema "$schema") :name (or (get schema "title") "UNKNOWN") :dependencies []}) 
; TODO: handle un-named entities better.

(defn- determine-nullability-for-column [schema column]
 (let [required-columns (set (get schema "required"))]
   (assoc column :nullable (not (contains? required-columns (:name column))))))

(defn- determine-nullabilty [schema res] 
 (assoc res :columns (mapv (partial determine-nullability-for-column schema) (:columns res))))

(defn- map-schema [schema]
 { :tables (vector 
     (->> schema
          extract-basics
          (extract-properties schema)
          (determine-nullabilty schema)))})

(defn parse-json-schema 
 "Accepts location, which should be coercible to a reader via clojure.java.io.reader,
  and returns an object formatted like a schema description used throughout Blutwurst."
 [location] 
 (with-open [r (io/reader location)]
  (let [json-document (json/parse-stream r)]
   (map-schema json-document))))

(defn parse-json-schema-from-spec [spec] 
 (-> spec
     :connection-string
     parse-json-schema))
