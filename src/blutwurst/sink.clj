(ns blutwurst.sink
   (:require [taoensso.timbre :as timbre :refer [trace]]))

(defn standard-output-sink [rows]
 (doseq [x rows]
  (println x)
 ))

(defn null-sink [rows] nil)

(defn make-sink [spec]
 (cond 
  (= "-" (:output spec)) standard-output-sink
  :else null-sink
 ))
