(ns blutwurst.tuple-generator
  (:import (java.util Random))
  (:require [taoensso.timbre :as timbre :refer [trace]]))

(defn- random-integer []
 (let [random (Random.)]
   (.nextInt random)
 ))

(defn- random-decimal []
 (let [random (Random.)]
  (.nextLong random)
))

(def value-generation-strategies
  ^{ :private true }
  [
   {
      :name "Simple String Generator"
      :determiner (fn [c] (= (:type c) "VARCHAR"))
      :generator (fn [c] "TEST")
   }
   {
       :name "Random Integer Generator"
       :determiner (fn [c] (= (:type c) "INTEGER"))
       :generator (fn [c] (random-integer)) ; TODO account for column's max value
   }
   {
       :name "Random Decimal Generator"
       :determiner (fn [c] (= (:type c) "DECIMAL"))
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
    {:table table-descriptor :tuples (repeatedly number-of-tuples table-generator)}
    ))

(defn generate-tuples-for-plan [execution-plan]
 (trace (pr-str execution-plan))
 (map #(generate-tuples-for-table % 100) execution-plan))
