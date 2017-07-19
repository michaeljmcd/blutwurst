(ns blutwurst.tuple-formatter
    (:require [clojure.data.csv :as csv]
              [taoensso.timbre :as timbre :refer [trace]]))

; TODO: handle file output
(defn- csv-formatter [spec rows]
 (csv/write-csv *out* (doall rows))

(def formatters 
  {
    :csv csv-formatter
  })

(defn format-rows [spec rows]
 (trace rows)
  (let [formatter (get formatters (:format spec))]
   (formatter spec rows)
    ))
