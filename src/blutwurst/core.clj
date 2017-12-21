(ns blutwurst.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]
            [taoensso.timbre :as timbre :refer [log  trace with-level]]
            [blutwurst.database :refer [retrieve-table-graph]]
            [blutwurst.jsonschema :refer [parse-json-schema-from-spec]]
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
   ["-d" "--directory OUTPUT_DIRECTORY" "Output directory to which to write individual table-named files."]
   ["-s" "--schema SCHEMA" "Database schemas to include in the database scan."
    :default []
    :assoc-fn accumulate-arguments]
   ["-t" "--table TABLE" "Database tables to include in the database scan. If provided, only listed tables are included."
    :default []
    :assoc-fn accumulate-arguments]
   ["-c" "--connection-string CONNECTION" "Connection string to scan. If a connection that is not a JDBC connection string is passed, it is assumed to be a JSON Schema instead."
    :default "jdbc:h2:mem:"]
   ["-f" "--format FORMAT" "Format to which test data should be exported. Valid options are csv, json, xml and sql."
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
   [nil "--generator-name NAME" "Specifies the name of a generator to be created through command line arguments."
    :default []
    :assoc-fn accumulate-arguments]
   [nil "--generator-regex REGEX" "Specifies a regex to be used when generating data."
    :default []
    :assoc-fn accumulate-arguments]
   [nil "--list-generators" "List out registered generators."]
   ["-i" "--ignore COLUMN" "Ignore a column entirely when generating data."
    :default []
    :assoc-fn accumulate-arguments]
   ["-v" "--verbose" :id :verbose]
   ["-h" "--help"]])

(defn build-spec [options]
  {:connection-string (:connection-string options)
   :format (:format options)
   :output-file (:output options)
   :output-directory (:directory options)
   :included-schemas (:schema options)
   :included-tables (:table options)
   :number-of-rows (:number-of-rows options)
   :column-generator-overrides (map #(hash-map :column-pattern %1 :generator-name %2)
                                    (:column options)
                                    (:generator options))
   :regex-generators (map #(hash-map :name %1 :regex %2) (:generator-name options) (:generator-regex options))
   :ignored-columns (:ignore options)})

(defn- exit-with-code [code]
  (System/exit code))

(defn- usage [option-summary]
  (println (->> ["Usage: blutwurst [options]"
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
  (exit-with-code 0))

(defn- print-generator-list [spec]
  (let [generators (retrieve-registered-generators spec)]
    (println (->> generators (string/join \newline)))
    (exit-with-code 0)))

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
    result))

(defn- print-parsing-error [parsed]
  (doseq [err (:errors parsed)]
    (println err))
  (exit-with-code 1))

(defn- discover-schema [spec]
  (if (string/starts-with? (:connection-string spec) "jdbc:")
    (retrieve-table-graph spec)
    (parse-json-schema-from-spec spec)))

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
        (not (empty? (:errors parsed-input))) (print-parsing-error parsed-input)
        (-> parsed-input :options :help) (usage (:summary parsed-input))
        (-> parsed-input :options :list-generators) (print-generator-list spec)
        (<= (count effective-args) 1) (usage (:summary parsed-input))
        :else (->> spec
                   discover-schema
                   create-data-generation-plan
                   (generate-tuples-for-plan spec)
                   (format-rows spec)
                   sink)))))
