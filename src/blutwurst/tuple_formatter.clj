(ns blutwurst.tuple-formatter
    (:require [clojure.data.csv :as csv]))

; TODO: handle file output
(defn- csv-formatter [spec rows]
 (csv/write-csv *out* rows))

(def formatters 
  {
    :csv csv-formatter
  })

(defn format-rows [spec rows]
  (let [formatter ((:format spec) formatters)]
    (formatter spec rows)
    ))
