(ns blutwurst.sink
  (:require [taoensso.timbre :as timbre :refer [trace]]))

(defn standard-output-sink [tables]
  (doseq [x tables]
    (println (apply str (:tuples x)))))

(defn null-sink [tables] nil)

(defn make-directory-sink [spec]
  (let [extension (name (:format spec))
        base-dir (:output-directory spec)]
    (fn [tables]
      (doseq [x tables]
        (let [path (str base-dir
                        java.io.File/separator
                        (or (-> x :entity :schema) "NOSCHEMA")
                        "_"
                        (-> x :entity :name)
                        "."
                        extension)]
          (spit path (apply str (:tuples x))))))))

(defn make-file-sink [spec]
  (let [path (:output-file spec)]
    (fn [tables]
      (spit path (apply str (flatten (map #(:tuples %) tables)))))))

(defn make-sink [spec]
  (cond
    (not (empty? (:output-directory spec))) (make-directory-sink spec)
    (= "-" (:output-file spec)) standard-output-sink
    (not (empty? (:output-file spec))) (make-file-sink spec)
    :else null-sink))
