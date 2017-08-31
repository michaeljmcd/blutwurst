(ns blutwurst.jsonschema
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.pprint :refer :all]
            [taoensso.timbre :as timbre :refer [trace]]))

(def ^:private unknown-count (atom 0))

(defn- create-name-for-entity []
  (swap! unknown-count inc)
  (str "UNKNOWN" @unknown-count))

(defn- extract-basics [hint schema]
  {:name (or (get schema "title") hint (create-name-for-entity)) :schema nil :dependencies []})

(defn- determine-type [prop]
  (let [prop-type (string/upper-case (or (get prop "type") ""))]
    (cond
      (and (empty? prop-type) (contains? prop "properties")) :complex
      (empty? prop-type) nil
      (= prop-type "STRING") :string
      (= prop-type "NUMBER") :decimal
      (= prop-type "INTEGER") :integer
      (= prop-type "OBJECT") :complex
      (= prop-type "ARRAY") :sequence)))

(defn- map-type [schema result]
  (assoc result :type (determine-type schema)))

(defn- build-constraints-for-property [schema prop]
  (let [property-name (first prop)
        required-properties (get schema "required")]
    {:nullable (nil? (some #(= property-name %) required-properties))}))

(declare map-schema)

(defn- create-property-from-entry [schema prop]
  (let [basic-data
        {:name (first prop) :type (determine-type (second prop)) :constraints (build-constraints-for-property schema prop)}]
    (cond
      (= (:type basic-data) :sequence)
      (assoc basic-data :properties (vector (map-schema "items" (get (second prop) "items"))))
      (= (:type basic-data) :complex)
      (assoc (map-schema (first prop) (second prop))  :constraints (build-constraints-for-property schema prop))
      :else
      basic-data)))

(defn- map-properties [schema result]
  (if (= (:type result) :sequence)
   (assoc result :properties (vector (map-schema nil (get schema "items"))))
  (assoc result 
         :properties
         (mapv (partial create-property-from-entry schema) 
                 (get schema "properties")))))

(defn- map-schema [hint schema]
  (->> schema
       (extract-basics hint)
       (map-type schema)
       (map-properties schema)))

(defn parse-json-schema
  "Accepts location, which should be coercible to a reader via clojure.java.io.reader,
  and returns an object formatted like a schema description used throughout Blutwurst."
  [location]
  (with-open [r (io/reader location)]
    (let [json-document (json/parse-stream r)
          hint (or (if (string? location) location nil) "UNKNOWN")]
      {:entities (list (map-schema nil json-document))})))

(defn parse-json-schema-from-spec [spec]
  (-> spec
      :connection-string
      parse-json-schema))
