(ns blutwurst.planner
    (:require [taoensso.timbre :as timbre :refer [trace]]
              [clojure.set :as setops]
              [clojure.pprint :refer :all]))

(defn- incoming-to-node [node edge]
 (and (= (:to-schema edge) (:schema node))
      (= (:to-name edge) (:name node))))

(defn- outgoing-from-node [node edge]
 (and (= (:from-schema edge) (:schema node))
      (= (:from-name edge) (:name node))))

(defn- find-nodes-without-incoming-connections [nodes edges]
 (if (empty? edges)
  nodes
  (filter #(not (some (fn [e] (incoming-to-node % e)) edges))
          nodes)
  ))

(defn- topological-schema-sort [all-nodes nonentrant-nodes edges result]
 (trace "Entering topological schema sort non-entrant: " (pr-str (seq nonentrant-nodes))) 
 (trace "Edges: " (pr-str (seq edges )))
 (trace "Result: " (pr-str (seq result)))

 (if (empty? nonentrant-nodes)
  (if (not (empty? edges))
   nil ; error
   result)

  (let [current-node (first nonentrant-nodes)
        nonentrant-nodes (rest nonentrant-nodes)
        result (cons current-node result)
        pruned-edges (filter (complement (partial outgoing-from-node current-node)) edges)]

    (trace "Edges after pruning: " (pr-str (seq pruned-edges)))

    (recur all-nodes
          (find-nodes-without-incoming-connections (setops/difference (set all-nodes) (set result)) pruned-edges)
           pruned-edges
           result)
  )))

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
