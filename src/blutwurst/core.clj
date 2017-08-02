(ns blutwurst.core
  (:require [clojure.tools.cli :refer [parse-opts]]
			[taoensso.timbre :as timbre
				:refer [log  trace  debug  info  warn  error  fatal  report
					logf tracef debugf infof warnf errorf fatalf reportf with-level
					spy get-env]]
            [blutwurst.database :refer [retrieve-table-graph]]
            [blutwurst.planner :refer [create-data-generation-plan]]
            [blutwurst.tuple-generator :refer [generate-tuples-for-plan]]
            [blutwurst.tuple-formatter :refer [format-rows]]
            [blutwurst.sink :refer [make-sink]])
  (:gen-class))

(def cli-options 
  [["-o" "--output OUTPUT_DIR" "Output location."
    :default "-"]
   ["-c" "--connection-string CONNECTION" "Connection string to scan."
     :default "jdbc:h2:mem:"]
   ["-f" "--format FORMAT" "Format to which test data should be exported."
    :parse-fn #(keyword %)
    :default :csv]
   ["-h" "--help"]])

(defn- build-spec [options] 
  {
   :connection-string (:connection-string options)
   :format (:format options)
   :output-location (:output options)
  })

(defn -main
  "Main command line interface that will pass in arguments and kick off the data generation process."
  [& args]
  (with-level :trace
      (let [parsed-input (parse-opts args cli-options)
            spec (build-spec (:options parsed-input))
            format-rows (partial format-rows spec)
            sink (make-sink spec)]
        (trace spec)

        (-> spec
            retrieve-table-graph
            create-data-generation-plan
            generate-tuples-for-plan
            format-rows
            sink)
        )))
