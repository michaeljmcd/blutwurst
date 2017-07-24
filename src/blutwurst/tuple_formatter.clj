(ns blutwurst.tuple-formatter
    (:require [clojure.data.csv :as csv]
              [taoensso.timbre :as timbre :refer [trace]]))

; TODO: handle file output
(defn- csv-formatter [spec rows]
 (trace rows)
 (let [values-only (map (fn [r] (map (fn [r2] (second r2)) r)) rows)]
     (csv/write-csv *out* values-only)
))

(def formatters 
  {
    :csv csv-formatter
  })

(defn format-rows [spec rows]
  (let [formatter (partial (get formatters (:format spec)) spec)]
   (map formatter rows)
    ))
