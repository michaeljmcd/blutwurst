(ns blutwurst.planner
    (:require [taoensso.timbre :as timbre :refer [trace]]
              [clojure.pprint :refer :all]))

(defn- find-nodes-without-incoming-connections [nodes edges]
 (filter #(some (complement (fn [e] 
                             (and (= (:to-schema e) (:schema %))
                                  (= (:to-name e) (:name %)))))
                 edges)
         nodes))

(defn- topological-schema-sort [all-nodes nonentrant-nodes edges result]
)

(defn- extract-edges [nodes]
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

  (topological-schema-sort nodes 
                           (find-nodes-without-incoming-connections nodes edges) 
                           edges 
                           '())
  ; TODO: error check
 ))
