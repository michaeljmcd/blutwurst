(ns blutwurst.tuple-generator
  (:require [taoensso.timbre :as timbre :refer [trace error]]
            [blutwurst.value-generators :refer [create-generators random-integer-under-value]]))

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
  (let [generators (create-generators spec)
        override-name (find-override-for-column spec column)]

    (if (nil? override-name)
      (generator-search column generators)
      (do 
        (trace "Found override " override-name " for column " column)
        (find-generator-by-name override-name generators))
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
