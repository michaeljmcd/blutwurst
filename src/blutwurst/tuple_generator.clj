(ns blutwurst.tuple-generator
  (:require [taoensso.timbre :as timbre :refer [trace error]]
            [clojure.pprint :refer :all]
            [blutwurst.value-generators :refer [create-generators random-integer-under-value]]))

(defn- generator-search [column strategies]
  (if (empty? strategies)
    nil
    (let [current-strategy (first strategies)]
      (if ((:determiner current-strategy) column)
        current-strategy
        (recur column (rest strategies))))))

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
        (find-generator-by-name override-name generators)))))

(defn- build-dependency-selector-fn [generated-data dependency]
  (let [targeted-table (first (filter #(and (= (-> % :entity :schema) (:target-schema dependency))
                                            (= (-> % :entity :name) (:target-name dependency)))
                                      generated-data))
        tuple-count (count (:tuples targeted-table))]
    (fn []
      (let [chosen-row (nth (:tuples targeted-table) (random-integer-under-value tuple-count))
            column-values (map #(hash-map (keyword (first %)) (get chosen-row (keyword (second %)))) (:links dependency))]
        (if (= :embedded (-> dependency :links first second))
          (hash-map (keyword (-> dependency :links first first)) chosen-row)
          (apply merge column-values))))))

(defn- value-columns [spec table]
  (let [linked-columns (flatten (map #(map first (:links %)) (:dependencies table)))]
    (remove #(or (some (fn [x] (= (:name %) x)) linked-columns)
                 (= :complex (:type %))
                 (= :sequence (:type %))
                 (some (fn [x] (re-matches (re-pattern x) (:name %))) (:ignored-columns spec)))
            (:properties table))))

(defn- dependency-contains-ignored-column [spec dep]
  (some (fn [x] (some #(let [re (re-pattern %)]
                         (or (re-matches re (first x))
                             (re-matches re (second x))))
                      (:ignored-columns spec)))
        (:links dep)))

(defn- filter-ignored-columns [spec dependencies]
  (remove (partial dependency-contains-ignored-column spec) dependencies))

(defn- complex-columns [table]
  (filter #(= :complex (:type %)) (:properties table)))

(defn- create-dependency-selection-generators [spec table generated-data]
  (map (partial build-dependency-selector-fn generated-data)
       (filter-ignored-columns spec (:dependencies table))))

(defn- create-value-generators [spec columns]
  (map #(fn [] (hash-map (-> % :name keyword) ((->> % (select-generators-for-column spec) :generator) %)))
       columns))

(declare build-generator-fn)

(defn- create-complex-property-generators [spec table generated-data]
  (map #(let [compound-generator (build-generator-fn spec % generated-data)]
          (fn [] (hash-map (-> % :name keyword) (compound-generator))))
       (complex-columns table)))

(defn- create-sequence-generators [spec table generated-data]
  (map #(fn [] (hash-map (-> % :name keyword) [1 2 3])) #_(fn [] (hash-map % :name keyword) (repeatedly 5 (create-complex-generator spec (-> table :properties first) generated-data)))
       (filter #(= :sequence (:type %)) (:properties table))))

(defn- create-top-level-generators [spec table]
  (if (some #{(:type table)} (list :string :decimal :datetime :integer))
    (create-value-generators spec (list table))))

(defn- create-complex-generator [spec table generated-data]
  (let [dependency-selectors (create-dependency-selection-generators spec table generated-data)
        value-generators (create-value-generators spec (value-columns spec table))
        self-generators (create-top-level-generators spec table)
        sequence-generators (create-sequence-generators spec table generated-data)
        complex-generators (create-complex-property-generators spec table generated-data)]
       ; TODO: log missing generators
    (fn []
      (->> (concat dependency-selectors 
                   complex-generators 
                   value-generators 
                   self-generators 
                   sequence-generators)
           (map #(%))
           (apply merge)))))

(def ^:private build-generator-fn
  (memoize create-complex-generator))

(defn- generate-tuples-for-table [table-descriptor spec generated-data]
  (let [table-generator (build-generator-fn spec table-descriptor generated-data)]
    {:entity table-descriptor :tuples (repeatedly (or (:number-of-rows spec) 10) table-generator)}))

(defn- generate-tuples-for-plan* [spec execution-plan result]
  (if (empty? execution-plan)
    (reverse result)
    (do
      (trace "Beginning data generation for table " (first execution-plan))
      (recur spec
             (rest execution-plan)
             (cons (generate-tuples-for-table (first execution-plan) spec result) result)))))

(defn generate-tuples-for-plan [spec execution-plan]
  (trace "Beginning tuple generation for execution plan.")
  (generate-tuples-for-plan* spec execution-plan '()))
