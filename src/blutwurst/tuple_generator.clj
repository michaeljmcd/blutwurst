(ns blutwurst.tuple-generator
  (:import (java.util Random))
  (:require [taoensso.timbre :as timbre :refer [trace error]]))

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

(def value-generation-strategies
  ^{ :private true }
  [
   {
      :name "Random String Generator"
      :determiner #(some #{(:type %)} '("VARCHAR" "NVARCHAR" "CHAR"))
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
  ])

(defn- select-generators-for-column [column]
  (let [generator-search (fn [column strategies]
                           (if (empty? strategies)
                             nil
                             (let [current-strategy (first strategies)]
                               (if ((:determiner current-strategy) column)
                                 current-strategy
                                 (recur column (rest strategies))
                                 ))
                             ))]

    (generator-search column value-generation-strategies)
    ))

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

(defn- build-generator-fn [table generated-data]
 (let [dependency-selectors (map (partial build-dependency-selector-fn generated-data) (:dependencies table))
       value-generators (map #(hash-map :column % :generator (-> % select-generators-for-column :generator)) 
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

(defn- generate-tuples-for-table [table-descriptor number-of-tuples generated-data]
  (let [table-generator (build-generator-fn table-descriptor generated-data)]
    {:table table-descriptor :tuples (repeatedly number-of-tuples table-generator)}
    ))

(defn- generate-tuples-for-plan* [rows-to-generate execution-plan result]
 (if (empty? execution-plan)
  (reverse result)
  (do
      (trace "Beginning data generation for table " (first execution-plan))
      (recur rows-to-generate
             (rest execution-plan)
             (cons (generate-tuples-for-table (first execution-plan) rows-to-generate result) result)))
 ))

(defn generate-tuples-for-plan [spec execution-plan]
 (trace "Beginning tuple generation for execution plan.")
 (generate-tuples-for-plan* (:number-of-rows spec) execution-plan '()))
 
