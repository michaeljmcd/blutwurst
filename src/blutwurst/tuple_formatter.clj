(ns blutwurst.tuple-formatter
    (:require [clojure.data.csv :as csv]
              [taoensso.timbre :as timbre :refer [trace]]))

(defn- csv-formatter [spec data]
 (trace data)
 (let [rows (:tuples data)
       values-only (mapv (fn [r] (mapv #(second %) r)) rows)]
       {:table (:table data)
        :tuples (with-out-str (csv/write-csv *out* values-only)) }
    ))

(def formatters 
  {
    :csv csv-formatter
  })

(defn format-rows [spec rows]
  (let [formatter (partial (get formatters (:format spec)) spec)]
   (map formatter rows)
    ))
