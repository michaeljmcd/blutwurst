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
        (is (= '({:table "test" :tuples ["1,2\n"]})
               (format-rows spec rows)))
      )))

(deftest sql-formatter-test
   (testing "Basic SQL generation with integer-only values."
     (let [spec {:format :sql}
           rows '({:table {:name "Example" :schema "foo" :columns ({:name "A" :type "INTEGER"} {:name "B" :type "INTEGER"})}
                   :tuples ((("A" 1) ("B" 2)))})]
             (is (= '({:table {:name "Example" :schema "foo" :columns ({:name "A" :type "INTEGER"} {:name "B" :type "INTEGER"})}
                      :tuples ["INSERT INTO \"foo\".\"Example\" (\"A\",\"B\") VALUES (1,2);\n"]})
                    (format-rows spec rows)))
           ))

   (testing "SQL generation quotes and escapes string values."
     (let [spec {:format :sql}
           rows '({:table {:name "Example" :schema "foo" :columns ({:name "A" :type "INTEGER"} {:name "B" :type "VARCHAR"})}
                   :tuples ((("A" 1) ("B" "Then O'Kelly came in with the \t hatchett.")))})]
             (is (= '({:table {:name "Example" :schema "foo" :columns ({:name "A" :type "INTEGER"} {:name "B" :type "VARCHAR"})}
                      :tuples ["INSERT INTO \"foo\".\"Example\" (\"A\",\"B\") VALUES (1,'Then O''Kelly came in with the \t hatchett.');\n"]})
                    (format-rows spec rows)))
   )))
