(ns blutwurst.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]
			[taoensso.timbre :as timbre :refer [log  trace with-level]]
            [blutwurst.database :refer [retrieve-table-graph]]
            [blutwurst.planner :refer [create-data-generation-plan]]
            [blutwurst.tuple-generator :refer [generate-tuples-for-plan]]
            [blutwurst.tuple-formatter :refer [format-rows]]
            [blutwurst.value-generators :refer [retrieve-registered-generators]]
            [blutwurst.sink :refer [make-sink]])
  (:import (java.util StringTokenizer))
  (:gen-class))

(def ^:private accumulate-arguments #(update-in %1 [%2] conj %3))

(def cli-options 
  [["-o" "--output OUTPUT_FILE" "Individual file to which to write the generated data."
    :default "-"]
   ["-K" "--config CONFIG_FILE" "Use options in CONFIG_FILE as though they were command line options."]
   ["-d" "--output-dir OUTPUT_DIRECTORY" "Output directory to which to write individual table-named files."]
   ["-s" "--schema SCHEMA" "Database schemas to include in the database scan."
    :default []
    :assoc-fn accumulate-arguments]
   ["-c" "--connection-string CONNECTION" "Connection string to scan."
     :default "jdbc:h2:mem:"]
   ["-f" "--format FORMAT" "Format to which test data should be exported. Valid options are csv, json and sql."
    :parse-fn #(keyword %)
    :default :csv]
   ["-n" "--number-of-rows NUMBER" "Number of rows to be generated for each table."
    :default 100
    :parse-fn #(Integer/parseInt %)
    :id :number-of-rows]
   [nil "--column PATTERN" "Specifies a Java-style regex to be used in assigning a generator to a column."
    :default []
    :assoc-fn accumulate-arguments]
   [nil "--generator NAME" "Specifies that columns matching the pattern given previously must use the generator name."
    :default []
    :assoc-fn accumulate-arguments]
   [nil "--list-generators" "List out registered generators."]
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
   :column-generator-overrides (map #(hash-map :column-pattern %1 :generator-name %2) 
                                    (:column options) 
                                    (:generator options)) ; TODO: add validation around this.
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

(defn- print-generator-list [spec]
 (let [generators (retrieve-registered-generators spec)]
  (println (->> generators (string/join \newline)))
  (System/exit 0)
 ))

; Base regex from
; https://stackoverflow.com/questions/366202/regex-for-splitting-a-string-using-space-when-not-surrounded-by-single-or-double
(defn- tokenize-file [input-text]
 (map #(cond
        (not (nil? (nth % 1))) (nth % 1)
        (not (nil? (nth % 2))) (nth % 2)
        :else (first %)) 
      (re-seq #"[^\s\"']+|\"([^\"]*)\"|'([^']*)'" input-text)))

(defn- derive-effective-arguments [args]
 (let [options (parse-opts args cli-options :no-defaults ["-K" "--config"])
       result (if (-> options :options :config)
                (concat args (-> options :options :config slurp tokenize-file))
                args)]
   (trace "Effective command-line arguments: " result)
   result
 ))

(defn -main
  "Main command line interface that will pass in arguments and kick off the data generation process."
  [& args]
      (let [effective-args (derive-effective-arguments args)
            parsed-input (parse-opts effective-args cli-options)
            spec (build-spec (:options parsed-input))
            sink (make-sink spec)]

          (with-level (if (-> parsed-input :options :verbose) :trace :fatal)
            (trace "Effective arguments after file handling: " (pr-str (seq effective-args)))
            (trace "Parsed arguments: " parsed-input)
            (trace "Finalized spec: " spec)

            (cond 
             (-> parsed-input :options :help) (usage (:summary parsed-input))
             (-> parsed-input :options :list-generators) (print-generator-list spec)
             :else (->> spec
                   retrieve-table-graph
                   create-data-generation-plan
                   (generate-tuples-for-plan spec)
                   (format-rows spec)
                   sink))
      )))
