(ns blutwurst.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]
			[taoensso.timbre :as timbre :refer [log  trace with-level]]
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
   ["-s" "--schema SCHEMA" "Database schemas to include in the database scan."
    :default '()
    :assoc-fn #(update-in %1 [%2] conj %3)]
   ["-c" "--connection-string CONNECTION" "Connection string to scan."
     :default "jdbc:h2:mem:"]
   ["-f" "--format FORMAT" "Format to which test data should be exported. Valid options are csv, json and sql."
    :parse-fn #(keyword %)
    :default :csv]
   ["-n" "--number-of-rows NUMBER" "Number of rows to be generated for each table."
    :default 100
    :parse-fn #(Integer/parseInt %)
    :id :number-of-rows
   ]
   ["-v" "--verbose" :id :verbose]
   ["-h" "--help"]])

(defn build-spec [options] 
  {
   :connection-string (:connection-string options)
   :format (:format options)
   :output-file (:output options)
   :output-directory (:output-dir options)
   :included-schemas (:schema options)
   :number-of-rows (:number-of-rows options)
  })

(defn- usage [option-summary]
 (println (->> ["Usage: java -jar blutwurst.jar [options]"
                ""
                option-summary
                ""
                "Blutwurst is a command line tool to generate test data."
                "Specifying a connection string will cause database table schemas to be scanned and test data randomly generated"
                "The output, format and output-dir options specify where to send the generated data (default is standard out)"
                "and in what format to send it (the default is CSV)."
                ""
                "Please report bugs or issues at https://github.com/michaeljmcd/blutwurst"]
                (string/join \newline)))
 (System/exit 0))

(defn -main
  "Main command line interface that will pass in arguments and kick off the data generation process."
  [& args]
      (let [parsed-input (parse-opts args cli-options)
            spec (build-spec (:options parsed-input))
            sink (make-sink spec)]

          (with-level (if (-> parsed-input :options :verbose) :trace :fatal)
            (trace parsed-input)
            (trace spec)

            (if (-> parsed-input :options :help)
              (usage (:summary parsed-input))

              (->> spec
                   retrieve-table-graph
                   create-data-generation-plan
                   generate-tuples-for-plan
                   (format-rows spec)
                   sink))
      )))
