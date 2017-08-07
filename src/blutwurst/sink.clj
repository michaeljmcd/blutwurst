(ns blutwurst.sink
   (:require [taoensso.timbre :as timbre :refer [trace]]))

(defn standard-output-sink [rows]
 (doseq [x rows]
  (println x)
 ))

(defn null-sink [rows] nil)

(defn make-directory-sink [spec]
 (let [extension (name (:format spec))
       base-dir (:output-directory spec)]
   (fn [rows]
     (doseq [x rows]
       (let [path (str base-dir 
                       java.io.File/separator 
                       (or (-> x :table :schema) "NOSCHEMA")
                       "_" 
                       (-> x :table :name) 
                       "." 
                       extension)]
           (spit path (apply str (:tuples x)))
       ))
     )))

(defn make-sink [spec]
 (cond 
  (not (empty? (:output-directory spec))) (make-directory-sink spec)
  (= "-" (:output-file spec)) standard-output-sink
  :else null-sink
 ))
