(ns blutwurst.value-generators
  (:import (java.util Random Date)
           (java.lang Math)
           (com.thedeanda.lorem LoremIpsum)
           (com.mifmif.common.regex Generex))
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [taoensso.timbre :as timbre :refer [trace error]]))

(defn- crop-to-column-size [column input-string]
 (if (or (nil? input-string) (empty? input-string))
  input-string
  (subs input-string 0 (min (count input-string) (or (:length column) 20)))
 ))

(defn- create-regex-generator [generator-name regex]
   { 
     :name generator-name
     :determiner #(do % nil)
     :generator #(let [g (Generex. regex)]
                    (crop-to-column-size % (.random g)))
   })

(defn- random-integer []
 (let [random (Random.)]
   (.nextInt random)
 ))

(defn random-integer-under-value [max-value]
  (let [random (Random.)]
     (mod (.nextLong random) max-value)
   ))

(defn- random-decimal []
 (let [random (Random.)]
  (.nextLong random)
))

(defn random-string [max-length]
 (let [g (Generex. "([A-Za-z0-9'\" \\n\\t!@#$%&*()])+")]
  (.random g (max 0 (- max-length (* 0.5 max-length))) max-length)))

(defn- random-datetime []
 (let [r (Random.)]
  (Date. (mod (.nextLong r) 4102466400000)) ; Makes sure that our date stays under 2100-01-01.
 ))

(defn- column-is-string? [column]
  (some #{(:type column)} '("VARCHAR" "NVARCHAR" "CHAR")))

(defn- max-integer-value-for-column [c]
 (cond 
  (= (:type c) "TINYINT") (long 255)
  (= (:type c) "SMALLINT") (long (- (Math/pow 2 15) 1))
  (= (:type c) "BIGINT") (long (- (Math/pow 2 63) 1))
  :else (long (- (Math/pow 2 32) 1))
 ))

(defn- make-resource-generator-fn [resource garbage-rows value-index]
 (let [values (drop garbage-rows (-> resource io/resource io/reader csv/read-csv))
       value-count (count values)]
  (trace values)
  (fn [c]
   (crop-to-column-size c (string/trimr (nth (nth values (random-integer-under-value value-count)) value-index))))
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
                  #(crop-to-column-size % (str (last-name-fn %) " " (first-name-fn %))))
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
       :name "Random City Generator"
       :determiner #(and (column-is-string? %) (string/includes? (:name %) "CITY"))
       :generator (let [l (LoremIpsum.)] (fn [c] (crop-to-column-size c (.getCity l))))
   }
   {
       :name "Random Email Generator"
       :determiner #(and (column-is-string? %) (string/includes? (:name %) "EMAIL"))
       :generator (let [l (LoremIpsum.)] (fn [c] (crop-to-column-size c (.getEmail l))))
   }
   {
      :name "Random Text Generator"
      :determiner column-is-string?
       :generator (let [l (LoremIpsum.)] (fn [c] (crop-to-column-size c (.getWords l 0 (or (:length c) 10)))))
   }
   {
      :name "Random String Generator"
      :determiner column-is-string?
      :generator #(random-string (or (min (:length %) 2000) 255))
   }
   {
       :name "Random Integer Generator"
       :determiner #(some #{(:type %)} '("INTEGER" "SMALLINT" "BIGINT" "INT" "TINYINT"))
       :generator #(random-integer-under-value (max-integer-value-for-column %))
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

(def create-generators (memoize (fn [spec]
                                 (->> value-generation-strategies
                                      (accrete-regex-generators spec)))))

(defn retrieve-registered-generators [spec] 
 (map :name (create-generators spec))) 
