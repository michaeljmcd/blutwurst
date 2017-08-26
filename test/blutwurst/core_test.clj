(ns blutwurst.core-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer :all]
            [clojure.tools.trace :as trace]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]
            [blutwurst.core :as core]
            [blutwurst.logging-fixture :refer :all]
            [blutwurst.in-memory-db-utilities :refer :all]))

(use-fixtures :each db-fixture logging-fixture)

(deftest option-parsing-tests
 (testing "Schema options accumulate."
  (let [args (list "test.jar" "-s" "S1" "-s" "S2")
        result (parse-opts args core/cli-options)]
    (is (= ["S1" "S2"] (-> result :options :schema)))
    (is (= ["S1" "S2"] (:included-schemas (core/build-spec (:options result)))))
  ))

  (testing "Parses options for row count."
    (let [args (list "test.jar" "-n" "30")
          result (parse-opts args core/cli-options)]
      (is (= 30 (-> result :options :number-of-rows)))
      (is (= 30 (:number-of-rows (core/build-spec (:options result)))))
    ))

  (testing "Parses option to list generators."
   (let [args (list "test.jar" "--list-generators")
         result (parse-opts args core/cli-options)]
    (is (= true (-> result :options :list-generators)))
  ))

  (testing "List generators is not the default."
    (let [args (list "test.jar" "--list-gens")
             result (parse-opts args core/cli-options)]
        (is (= nil (-> result :options :list-generators)))
      )))

(deftest spec-building-tests
  (testing "Builds out included tables list."
    (let [args (list "test.jar" "--table" "T1" "-s" "a" "-t" "T2")
          result (core/build-spec (:options (parse-opts args core/cli-options)))]
      (is (= (list "T1" "T2") (:included-tables result)))
      ))

 (testing "Output directory chosen from correct input."
  (let [args (list "test.jar" "--column" "ASDF" "--output-dir" "foo" "--directory" "baz")
        result (core/build-spec (:options (parse-opts args core/cli-options)))]
   (is (= "baz" (:output-directory result)))
  ))

 (testing "Handles column - generator pairs."
  (let [args (list "test.jar" "--column" "ASDF" "--generator" "foobar" "--column" "123" "--generator" "baz")
        result (core/build-spec (:options (parse-opts args core/cli-options)))]
    (is (= (list {:column-pattern "ASDF" :generator-name "foobar"} 
             {:column-pattern "123" :generator-name "baz"})
           (:column-generator-overrides result)))
    ))

 (testing "Handles receiving too many columns"
    (let [args (list "test.jar" "--column" "ASDF" "--generator" "foobar" "--column" "baz")
            result (core/build-spec (:options (parse-opts args core/cli-options)))]
        (is (= (list {:column-pattern "ASDF" :generator-name "foobar"})
               (:column-generator-overrides result)))
        ))

 (testing "Handles receiving too many generators."
    (let [args (list "test.jar" "--column" "ASDF" "--generator" "foobar" "--generator" "baz")
            result (core/build-spec (:options (parse-opts args core/cli-options)))]
        (is (= (list {:column-pattern "ASDF" :generator-name "foobar"})
               (:column-generator-overrides result)))
        ))

 (testing "Handles regex generator argument pairs."
  (let [args (list "test.jar" "--generator-name" "ASDF" "--generator-regex" "foobar" "--generator-name" "123" "--generator-regex" "baz")
        result (core/build-spec (:options (parse-opts args core/cli-options)))]
    (is (= (list {:name "ASDF" :regex "foobar"} 
                 {:name "123" :regex "baz"})
           (:regex-generators result)))
    ))
 )

(deftest integration-tests
  (testing "Handles bad options."
    (with-redefs-fn {#'core/exit-with-code (fn [c] c)}
    #(let [output (with-out-str (core/-main "app.jar" "--darby-ogill-and-the-little-people" "sputnik"))]
      (pprint output)
      (is (= "Unknown option: \"--darby-ogill-and-the-little-people\"" (string/trim output)))
      )))

  (testing "End to end flow."
   (let [output (with-out-str (core/-main "app.jar" "-c" connection-string "-f" "csv" "-o" "-" "-n" "2"))]
    (is (and (string/includes? output "CATEGORY,NAME")
             (string/includes? output "ID,NAME")
             (string/includes? output "ID,PURCHASEDBYID,PURCHASETYPECATEGORY,AMOUNT,PURCHASETYPENAME")
             (= 11 (count (string/split-lines output) )))
     ))
  ))
