(ns blutwurst.sink)

(defn standard-output-sink [rows]
 (doseq [x rows]
  (println x)
 ))

(defn make-sink [spec]
 (cond 
  (= "-" (:output spec)) standard-output-sink
  :else standard-output-sink
 ))
