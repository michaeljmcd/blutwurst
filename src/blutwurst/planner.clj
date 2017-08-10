(ns blutwurst.planner
    (:require [taoensso.timbre :as timbre :refer [trace]]
              [clojure.pprint :refer :all]))

(defn topological-schema-sort [nodes edges result]
)

(defn extract-edges [nodes]
 (->> nodes
      (map #(map (fn [d] {
                      :from-name (:name %) 
                      :from-schema (:schema %) 
                      :to-name (:target-name d) 
                      :to-schema (:target-schema d)
                      }) 
             (:dependencies %)))
      flatten
      distinct))

(defn create-data-generation-plan [schema]
 (trace schema)

 (let [nodes (:tables schema)
       edges (extract-edges nodes)]
  (pprint edges)

  (topological-schema-sort nodes edges [])
  ; TODO: error check
 ))
