(ns blutwurst.tuple-formatter
    (:require [clojure.data.csv :as csv]
              [taoensso.timbre :as timbre :refer [trace]]))

; TODO: handle file output
(defn- csv-formatter [spec rows]
 (let [values-only (mapv (fn [r] (mapv #(second %) r)) rows)]
     (csv/write-csv *out* values-only)
))

(def formatters 
  {
    :csv csv-formatter
  })

(defn format-rows [spec rows]
  (let [formatter (partial (get formatters (:format spec)) spec)]
   (doseq [x rows]
    (formatter x))
    ))