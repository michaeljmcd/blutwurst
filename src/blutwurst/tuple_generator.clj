(ns blutwurst.tuple-generator
  (:require [clojure.pprint :refer :all]))

(def value-generation-strategies
  ^{ :private true }
  [
   {
      :name "Simple String Generator"
      :determiner (fn [c] (= (:type c) "VARCHAR"))
      :generator (fn [c] "TEST")
   }
   {
       :name "Dumb Integer Generator"
       :determiner (fn [c] (= (:type c) "INTEGER"))
       :generator (fn [c] 1)
   }
  ])

(defn- select-generators-for-column [column]
  (let [generator-search (fn [column strategies]
                           (if (nil? strategies)
                             nil
                             (let [current-strategy (first strategies)]
                              (pprint column)
                              (pprint current-strategy)
                               (if ((:determiner current-strategy) column)
                                 current-strategy
                                 (recur column (rest strategies))
                                 ))
                             ))]
    (generator-search column value-generation-strategies)
    ))

(defn- build-generator [table]
  (let [column-generators (map (fn [i] 
                                 (assoc i :generator (:generator (select-generators-for-column i))
                                       )) 
                               (:columns table))]
    (fn []
      (map (fn [i]
             (list (:name i)
                   ((:generator i) (:column i))
                   ))
           column-generators))
  ))

(defn generate-tuples-for-table [table-descriptor number-of-tuples]
  (let [table-generator (build-generator table-descriptor)]
    (repeatedly number-of-tuples table-generator)
    ))

(defn generate-tuples-for-plan [execution-plan]
 (map (fn [a] (generate-tuples-for-table a 100)) execution-plan))
