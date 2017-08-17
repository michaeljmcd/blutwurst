(ns blutwurst.tuple-formatter-test
  (:import (java.util Date))
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer :all]
            [blutwurst.logging-fixture :refer :all]
            [blutwurst.tuple-formatter :refer :all]))

(use-fixtures :each logging-fixture)

(deftest csv-formatter-test
  (testing "Generating a CSV from rows."
    (let [spec {:format :csv}
          table {:name "Example" :schema "foo" :columns '({:name "A" :type "INTEGER"} {:name "B" :type "INTEGER"})}
          rows `({:table ~table :tuples ({:A 1 :B 2})})]
        (is (= `[{:table ~table :tuples ["A,B\n1,2\n"]}]
               (format-rows spec rows)))
      )))

(deftest sql-formatter-test
   (testing "Basic SQL generation with integer-only values."
     (let [spec {:format :sql}
           table {:name "Example" :schema "foo" :columns '({:name "A" :type "INTEGER"} {:name "B" :type "INTEGER"})}
           rows `({:table ~table
                   :tuples ({:A 1 :B 2})})]
             (is (= '({:table {:name "Example" :schema "foo" :columns ({:name "A" :type "INTEGER"} {:name "B" :type "INTEGER"})}
                      :tuples ["INSERT INTO \"foo\".\"Example\" (\"A\",\"B\") VALUES (1,2);\n"]})
                    (format-rows spec rows)))
           ))

   (testing "SQL generation quotes and escapes string values."
     (let [spec {:format :sql}
           table {:name "Example" :schema "foo" :columns '({:name "A" :type "INTEGER"} {:name "B" :type "VARCHAR"})}
           rows `({:table ~table
                   :tuples ({:A 1 :B  "Then O'Kelly came in with the \t hatchett."})})]
             (is (= `({:table ~table
                      :tuples ["INSERT INTO \"foo\".\"Example\" (\"A\",\"B\") VALUES (1,'Then O''Kelly came in with the \t hatchett.');\n"]})
                    (format-rows spec rows)))
     ))

     (testing "SQL generation formats DATE values."
       (let [spec {:format :sql}
           table {:name "Example" :schema "foo" :columns '({:name "A" :type "INTEGER"} {:name "B" :type "DATE"})}
           rows `({:table ~table
                   :tuples ({:A 1 :B ~(Date. 1109741401000)})
                  })]
             (is (= `({:table ~table
                      :tuples ["INSERT INTO \"foo\".\"Example\" (\"A\",\"B\") VALUES (1,'2005-03-01T23:30:01.000-06:00');\n"]})
                  ; TODO: handle timezone in test
                    (format-rows spec rows))))
     ))

(deftest json-formatter-test
  (testing "Basic JSON generation."
     (let [spec {:format :json}
           table {:name "Example" :schema "foo" :columns '({:name "A" :type "INTEGER"} {:name "B" :type "VARCHAR"})}
           rows `({:table ~table
                   :tuples ({:A 1 :B  "\"Thus sayeth...\""})})]
       (is (= `[{:table ~table
                 :tuples ["[{\"A\":1,\"B\":\"\\\"Thus sayeth...\\\"\"}]"]}]
              (format-rows spec rows))
           ))
  ))
