(ns blutwurst.tuple-formatter
    (:require [clojure.data.csv :as csv]
              [taoensso.timbre :as timbre :refer [trace]]))

(defn- csv-formatter [spec rows]
 (let [values-only (mapv (fn [r] (mapv #(second %) r)) rows)]
   (with-out-str 
     (csv/write-csv *out* values-only))
))

(def formatters 
  {
    :csv csv-formatter
  })

(defn format-rows [spec rows]
  (let [formatter (partial (get formatters (:format spec)) spec)]
   (map formatter rows)
    ))
