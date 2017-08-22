(ns blutwurst.value-generators
  (:import (java.util Random Date)
           (com.mifmif.common.regex Generex))
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [taoensso.timbre :as timbre :refer [trace error]]))

(defn- create-regex-generator [generator-name regex]
   { 
     :name generator-name
     :determiner #(do % nil)
     :generator #(let [g (.Generex regex)]
                    (.random g))
   })

(defn- random-integer []
 (let [random (Random.)]
   (.nextInt random)
 ))

(defn random-integer-under-value [value]
  (let [random (Random.)]
     (.nextInt random value)
   ))

(defn- random-decimal []
 (let [random (Random.)]
  (.nextLong random)
))

(defn random-string [max-length]
 (let [r (Random.)
       valid-chars (map char (range 97 123)) ; TODO make this a little cleaner
       max-char-index (- (count valid-chars) 1)]
    (apply str
    (repeatedly (.nextInt r (- max-length 1))
                #(nth valid-chars (.nextInt r max-char-index))))
 ))

(defn- random-datetime []
 (let [r (Random.)]
  (Date. (mod (.nextLong r) 4102466400000)) ; Makes sure that our date stays under 2100-01-01.
 ))

(defn- column-is-string? [column]
  (some #{(:type column)} '("VARCHAR" "NVARCHAR" "CHAR")))

(defn- make-resource-generator-fn [resource garbage-rows value-index]
 (let [values (drop garbage-rows (-> resource io/resource io/reader csv/read-csv))
       value-count (count values)]
  (trace values)
  (fn [c]
   (string/trimr (nth (nth values (random-integer-under-value value-count)) value-index)))
 ))

(def value-generation-strategies
  ^{ :private true }
  [
   { 
     :name "Random State Abbreviation Selector (U.S. and Canada)"
     :determiner #(and (column-is-string? %) (string/includes? (:name %) "STATE"))
     :generator (make-resource-generator-fn "state_table.csv" 1 2)
   }
   { 
     :name "Random State Name Selector (U.S. and Canada)"
     :determiner #(do % nil)
     :generator (make-resource-generator-fn "state_table.csv" 1 1)
   }
   {
     :name "Random Full Name Selector (U.S.)"
     :determiner #(and (column-is-string? %) (string/includes? (:name %) "FULL") (string/includes? (:name %) "NAME"))
     :generator (let [last-name-fn (make-resource-generator-fn "Names_2010Census_Top1000.csv" 3 0)
                      first-name-fn (make-resource-generator-fn "census-derived-all-first.csv" 0 0)]
                  #(str (last-name-fn %) " " (first-name-fn %)))
   }
   {
     :name "Random First Name Selector (U.S.)"
     :determiner #(and (column-is-string? %) (string/includes? (:name %) "FIRST"))
     :generator (make-resource-generator-fn "census-derived-all-first.csv" 0 0)
   }
   {
      :name "Random Last Name Selector (U.S.)"
      :determiner #(and (column-is-string? %) (string/includes? (:name %) "LAST"))
      :generator (make-resource-generator-fn "Names_2010Census_Top1000.csv" 3 0)
   }
   {
      :name "Random String Generator"
      :determiner column-is-string?
      :generator #(random-string (or (min (:length %) 2000) 255))
   }
   {
       :name "Random Integer Generator"
       :determiner #(some #{(:type %)} '("INTEGER" "SMALLINT" "BIGINT"))
       :generator (fn [c] (random-integer)) ; TODO account for column's max value
   }
   {
       :name "Random Decimal Generator"
       :determiner #(some #{(:type %)} '("DECIMAL" "DOUBLE"))
       :generator (fn [c] (random-decimal)) ; TODO account for column's max value
   }
   {
       :name "Random Date / Timestamp Generator"
       :determiner #(some #{(:type %)} '("DATE" "DATETIME" "TIMESTAMP"))
       :generator (fn [c] (random-datetime))
   }
  ])

(defn- accrete-regex-generators [spec generators]
 (concat (map #(create-regex-generator (:name %) (:regex %)) (:regex-generators spec))
          generators))

(defn create-generators [spec] ; TODO: memoize?
 (->> value-generation-strategies
      (accrete-regex-generators spec)))

(defn retrieve-registered-generators [spec] 
 (map :name (create-generators spec))) 
