(ns blutwurst.planner
    (:require [taoensso.timbre :as timbre :refer [trace]]
              [clojure.set :as setops]
              [clojure.pprint :refer :all]))

(defn- find-nodes-without-incoming-connections [nodes edges]
 (if (empty? edges)
  nodes
 (filter #(some (complement (fn [e] 
                             (and (= (:to-schema e) (:schema %))
                                  (= (:to-name e) (:name %)))))
                 edges)
         nodes)))

(defn- incoming-to-node [node edge]
 (and (= (:to-schema edge) (:schema node))
      (= (:to-name edge) (:name edge))))

(defn- topological-schema-sort [all-nodes nonentrant-nodes edges result]
 (trace "Entering topological schema sort non-entrant: " (pr-str (seq nonentrant-nodes))) 
        (trace "Edges: " (pr-str (seq edges )))
        (trace "Result: " (pr-str (seq result)))
        ;(trace "All nodes: " (pr-str (seq all-nodes)))

 (if (empty? nonentrant-nodes)
  (if (not (empty? edges))
   nil ; error
   result)

  (let [current-node (first nonentrant-nodes)
        nonentrant-nodes (rest nonentrant-nodes)
        result (cons current-node result)
        pruned-edges (filter (partial incoming-to-node current-node) edges)
       ]

    (trace "Edges after pruning: " (pr-str (seq pruned-edges)))

    (recur all-nodes
          (find-nodes-without-incoming-connections (setops/difference (set all-nodes) (set result)) pruned-edges)
           pruned-edges
           result)
  ))
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
  (trace "Preparing to sort graph....")

  (topological-schema-sort nodes 
                           (find-nodes-without-incoming-connections nodes edges) 
                           edges 
                           '())
  ; TODO: error check
 ))
