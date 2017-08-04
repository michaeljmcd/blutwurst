(ns blutwurst.tuple-formatter-test
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer :all]
            [blutwurst.logging-fixture :refer :all]
            [blutwurst.tuple-formatter :refer :all]))

(use-fixtures :each logging-fixture)

(deftest csv-formatter-test
  (testing "Generating a CSV from rows."
    (let [spec {:format :csv}
          rows '({:table "test" :tuples ((("A" 1) ("B" 2)))})]
        (is (= '({:table "test" :tuples "1,2\n"})
               (format-rows spec rows)))
      )))

(deftest sql-formatter-test
   (testing "Basic SQL generation."
     (let [spec {:format :sql}
           rows '({:table {:name "Example" :schema "foo" :columns ({:name "A" :type "INTEGER"} {:name "B" :type "INTEGER"})}
                   :tuples ((("A" 1) ("B" 2)))})]
             (is (= '("INSERT INTO \"foo\".\"Example\" (A, B) VALUES (1, 2)")
                    (format-rows spec rows)))
           )))
