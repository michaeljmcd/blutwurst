(ns blutwurst.tuple-generator
  (:import (java.util Random Date))
  (:require [taoensso.timbre :as timbre :refer [trace error]]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clojure.string :as string]))

(defn- random-integer []
 (let [random (Random.)]
   (.nextInt random)
 ))

(defn- random-integer-under-value [value]
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

(defn- generator-search [column strategies]
   (if (empty? strategies)
     nil
     (let [current-strategy (first strategies)]
       (if ((:determiner current-strategy) column)
         current-strategy
         (recur column (rest strategies))
         ))
     ))

(defn- find-override-for-column [spec column]
 (->> (:column-generator-overrides spec)
      (filter #(re-matches (re-pattern (:column-pattern %)) (:name column)))
      first
      :generator-name))

(defn- find-generator-by-name [generator-name strategies]
 (->> strategies
      (filter #(= (:name %) generator-name))
      first))

(defn- select-generators-for-column [spec column]
  (let [override-name (find-override-for-column spec column)]

    (if (nil? override-name)
      (generator-search column value-generation-strategies)
      (do 
        (trace "Found override " override-name " for column " column)
        (find-generator-by-name override-name value-generation-strategies))
    )))

(defn- build-dependency-selector-fn [generated-data dependency]
  (let [targeted-table (first (filter #(and (= (-> % :table :schema) (:target-schema dependency))
                                   (= (-> % :table :name) (:target-name dependency)))
                             generated-data))
        tuple-count (count (:tuples targeted-table))]
    (fn []
      (let [chosen-row (nth (:tuples targeted-table) (random-integer-under-value tuple-count))
            column-values (map #(list (keyword (first %)) (get chosen-row (keyword (second %)))) (:links dependency))]
        column-values
    ))
 ))

(defn- value-columns [table]
 (let [linked-columns (flatten (map #(map first (:links %)) (:dependencies table)))]
  (remove #(some (fn [x] (= (:name %) x)) linked-columns) (:columns table))
 ))

(defn- build-generator-fn [spec table generated-data]
 (let [dependency-selectors (map (partial build-dependency-selector-fn generated-data) (:dependencies table))
       value-generators (map #(hash-map :column % 
                                        :generator (->> % (select-generators-for-column spec) :generator)) 
                             (value-columns table))]

    (doseq [c value-generators]
     (if (nil? (:generator c))
     (error "Unable to find generator for column " (:column c))))

    (fn []
     (apply hash-map
            (flatten (concat (map #(%) dependency-selectors)
                             (map (fn [a] (list (-> a :column :name keyword) 
                                                ((:generator a) (:column a)))) 
                                  value-generators)
              )))
    )
  ))

(defn- generate-tuples-for-table [table-descriptor spec generated-data]
  (let [table-generator (build-generator-fn spec table-descriptor generated-data)]
    {:table table-descriptor :tuples (repeatedly (:number-of-rows spec) table-generator)}
    ))

(defn- generate-tuples-for-plan* [spec execution-plan result]
 (if (empty? execution-plan)
  (reverse result)
  (do
      (trace "Beginning data generation for table " (first execution-plan))
      (recur spec
             (rest execution-plan)
             (cons (generate-tuples-for-table (first execution-plan) spec result) result)))
 ))

(defn generate-tuples-for-plan [spec execution-plan]
 (trace "Beginning tuple generation for execution plan.")
 (generate-tuples-for-plan* spec execution-plan '()))
 
(defn retrieve-registered-generators [] 
 (map :name value-generation-strategies)) 
