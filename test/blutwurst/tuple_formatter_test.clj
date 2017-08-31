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
          table {:name "Example" :schema "foo" :properties '({:name "A" :type :integer} {:name "B" :type :integer})}
          rows `({:table ~table :tuples ({:A 1 :B 2})})]
      (is (= `[{:table ~table :tuples ["A,B\n1,2\n"]}]
             (format-rows spec rows)))))

  (testing "Flattens out embedded objects."
    (let [spec {:format :csv}
          person-table {:name "Person" :schema nil :properties [{:name "Address" :type :complex} {:name "Name" :type :string}]
                        :dependencies [{:target-schema nil :target-name "Address" :dependency-name nil :links {"Address" :embedded}}]}
          data `({:table ~person-table :tuples ~(list {:name "John Doe" :Address {:Address1 "123 Main" :City "Springfield"}})})]
      (is (= [{:table person-table :tuples ["name,Address.Address1,Address.City\nJohn Doe,123 Main,Springfield\n"]}]
             (format-rows spec data))))))

(deftest sql-formatter-test
  (testing "Basic SQL generation with integer-only values."
    (let [spec {:format :sql}
          table {:name "Example" :schema "foo" :properties '({:name "A" :type :integer} {:name "B" :type :integer})}
          rows `({:table ~table
                  :tuples ({:A 1 :B 2})})]
      (is (= '({:table {:name "Example" :schema "foo" :properties ({:name "A" :type :integer} {:name "B" :type :integer})}
                :tuples ["INSERT INTO \"foo\".\"Example\" (\"A\",\"B\") VALUES (1,2);\n"]})
             (format-rows spec rows)))))

  (testing "SQL generation quotes and escapes string values."
    (let [spec {:format :sql}
          table {:name "Example" :schema "foo" :properties '({:name "A" :type :integer} {:name "B" :type :string})}
          rows `({:table ~table
                  :tuples ({:A 1 :B  "Then O'Kelly came in with the \t hatchett."})})]
      (is (= `({:table ~table
                :tuples ["INSERT INTO \"foo\".\"Example\" (\"A\",\"B\") VALUES (1,'Then O''Kelly came in with the \t hatchett.');\n"]})
             (format-rows spec rows)))))

  (testing "SQL generation flattens out embedded objects."
    (let [spec {:format :sql}
          person-table {:name "Person" :schema nil :properties [{:name "Address" :type :complex} {:name "Name" :type :string}]
                        :dependencies [{:target-schema nil :target-name "Address" :dependency-name nil :links {"Address" :embedded}}]}
          data `({:table ~person-table :tuples ~(list {:Name "John Doe" :Address {:Address1 "123 Main" :City "Springfield"}})})]
      (is (= [{:table person-table :tuples ["INSERT INTO \"Person\" (\"Name\",\"Address.Address1\",\"Address.City\") VALUES ('John Doe','123 Main','Springfield');\n"]}]
             (format-rows spec data)))))

  (testing "SQL generation formats DATE values."
    (let [spec {:format :sql}
          table {:name "Example" :schema "foo" :properties '({:name "A" :type :integer} {:name "B" :type :datetime})}
          rows `({:table ~table
                  :tuples ({:A 1 :B ~(Date. 1109741401000)})})]
      (is (= `({:table ~table
                :tuples ["INSERT INTO \"foo\".\"Example\" (\"A\",\"B\") VALUES (1,'2005-03-01T23:30:01.000-06:00');\n"]})
                  ; TODO: handle timezone in test
             (format-rows spec rows))))))

(deftest json-formatter-test
  (testing "Basic JSON generation."
    (let [spec {:format :json}
          table {:name "Example" :schema "foo" :properties '({:name "A" :type :integer} {:name "B" :type :string})}
          rows `({:table ~table
                  :tuples ({:A 1 :B  "\"Thus sayeth...\""})})]
      (is (= `[{:table ~table
                :tuples ["[{\"A\":1,\"B\":\"\\\"Thus sayeth...\\\"\"}]"]}]
             (format-rows spec rows))))))
