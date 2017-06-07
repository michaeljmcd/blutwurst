(ns blutwurst.tuple-generator)

(def value-generation-strategies
  ^{ :private true }
  [
   {
      :name "Simple String Generator"
      :determiner (fn [c] (= (:type c) "VARCHAR"))
      :generator (fn [c] "TEST")
   }
  ])

(defn generate-tuples-for-table [table-descriptor number-of-tuples]
  (let [table-generator (fn [] { :a "1" })]
    (repeatedly number-of-tuples table-generator)
    ))
