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
  [["-o" "--output OUTPUT_FILE" "Individual file to which to write the generated data."
    :default "-"]
   ["-d" "--output-dir OUTPUT_DIRECTORY" "Output directory to which to write individual table-named files."]
   ["-c" "--connection-string CONNECTION" "Connection string to scan."
     :default "jdbc:h2:mem:"]
   ["-f" "--format FORMAT" "Format to which test data should be exported."
    :parse-fn #(keyword %)
    :default :csv]
   ["-v" "--verbose" :id :verbose]
   ["-h" "--help"]])

(defn- build-spec [options] 
  {
   :connection-string (:connection-string options)
   :format (:format options)
   :output-file (:output options)
   :output-directory (:output-dir options)
  })

(defn -main
  "Main command line interface that will pass in arguments and kick off the data generation process."
  [& args]
      (let [parsed-input (parse-opts args cli-options)
            spec (build-spec (:options parsed-input))
            format-rows (partial format-rows spec)
            sink (make-sink spec)]
      (with-level (if (-> parsed-input :options :verbose) :trace :fatal)
        (trace parsed-input)
        (trace spec)

        (-> spec
            retrieve-table-graph
            create-data-generation-plan
            generate-tuples-for-plan
            format-rows
            sink)
        )))
