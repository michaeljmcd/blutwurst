(ns blutwurst.tuple-formatter)

(defn- csv-formatter [spec rows]
  )

(def formatters 
  {
    :csv csv-formatter
  })

(defn format-rows [spec rows]
  (let [formatter ((:format spec) formatters)]
    (formatter spec rows)
    ))
