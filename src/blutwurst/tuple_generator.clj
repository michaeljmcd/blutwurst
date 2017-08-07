(ns blutwurst.tuple-generator
  (:import (java.util Random))
  (:require [taoensso.timbre :as timbre :refer [trace]]
            [clojure.pprint :refer :all]))

(defn- random-integer []
 (let [random (Random.)]
   (.nextInt random)
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
      :determiner #(= (:type %) "VARCHAR")
      :generator #(random-string (or (:length %) 255))
   }
   {
       :name "Random Integer Generator"
       :determiner #(= (:type %) "INTEGER")
       :generator (fn [c] (random-integer)) ; TODO account for column's max value
   }
   {
       :name "Random Decimal Generator"
       :determiner #(= (:type %) "DECIMAL")
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
  (let [column-generators (map (fn [column] 
                                 (assoc column :generator (:generator (select-generators-for-column column))
                                       )) 
                               (:columns table))]
    (fn []
      (zipmap (map #(-> % :name keyword) column-generators)
              (map #((:generator %) (:column %)) column-generators))
      )
  ))

(defn generate-tuples-for-table [table-descriptor number-of-tuples]
  (let [table-generator (build-generator table-descriptor)]
    {:table table-descriptor :tuples (repeatedly number-of-tuples table-generator)}
    ))

(defn generate-tuples-for-plan [execution-plan]
 (trace (pr-str execution-plan))
 (map #(generate-tuples-for-table % 100) execution-plan))
