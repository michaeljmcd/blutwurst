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
            column-values (map #(list (keyword (first %)) (get chosen-row (keyword (second %)))) (:links dependency))]
        (if (= :embedded (-> dependency :links first second))
          (list (keyword (-> dependency :links first first)) chosen-row)
          column-values)))))

(defn- value-columns [spec table]
  (let [linked-columns (flatten (map #(map first (:links %)) (:dependencies table)))]
    (remove #(or (some (fn [x] (= (:name %) x)) linked-columns)
                 (= :complex (:type %))
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

(defn- create-value-generators [spec table]
    (map #(fn[] (list (-> % :name keyword) ((->> % (select-generators-for-column spec) :generator) %)))
                                     (value-columns spec table)))

(declare build-generator-fn)

(defn- create-complex-generators [spec table generated-data]
    (map #(let [compound-generator (build-generator-fn spec % generated-data)]
                                          (fn [] (list (-> % :name keyword) (compound-generator))))
                                       (complex-columns table)))

(def ^:private build-generator-fn 
 (memoize 
  (fn [spec table generated-data]
     (let [dependency-selectors (create-dependency-selection-generators spec table generated-data)
           value-generators (create-value-generators spec table)
           complex-generators (create-complex-generators spec table generated-data)]
       (doseq [c value-generators]
         (if (nil? (:generator c))
           (error "Unable to find generator for column " (:column c))))

       (fn []
         (->> (concat dependency-selectors complex-generators value-generators)
              (map #(%))
              flatten
              (apply hash-map)))))))

(defn- generate-tuples-for-table [table-descriptor spec generated-data]
  (let [table-generator (build-generator-fn spec table-descriptor generated-data)]
    {:entity table-descriptor :tuples (repeatedly (:number-of-rows spec) table-generator)}))

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
