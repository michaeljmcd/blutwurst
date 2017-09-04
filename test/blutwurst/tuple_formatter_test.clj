(ns blutwurst.tuple-formatter-test
  (:import (java.util Date))
  (:require [clojure.test :refer :all]
            [clojure.pprint :refer :all]
            [blutwurst.logging-fixture :refer :all]
            [blutwurst.tuple-formatter :refer :all]))

(use-fixtures :each logging-fixture)

(def simple-schema {:name "Example" :schema "foo" :properties [{:name "A" :type :integer} {:name "B" :type :string}]})
(def person-schema {:name "Person" :schema nil :properties [{:name "Address" :type :complex} {:name "Name" :type :string}]
                        :dependencies [{:target-schema nil :target-name "Address" :dependency-name nil :links {"Address" :embedded}}]})

(deftest csv-formatter-test
  (testing "Generating a CSV from rows."
    (let [spec {:format :csv}
          table (assoc-in simple-schema [:properties 1 :type] :integer)
          rows `({:entity ~table :tuples ({:A 1 :B 2})})]
      (is (= `[{:entity ~table :tuples ["A,B\n1,2\n"]}]
             (format-rows spec rows)))))

  (testing "Flattens out embedded objects."
    (let [spec {:format :csv}
          data `({:entity ~person-schema :tuples ~(list {:name "John Doe" :Address {:Address1 "123 Main" :City "Springfield"}})})]
      (is (= [{:entity person-schema :tuples ["name,Address.Address1,Address.City\nJohn Doe,123 Main,Springfield\n"]}]
             (format-rows spec data))))))

(deftest sql-formatter-test
  (testing "Basic SQL generation with integer-only values."
    (let [spec {:format :sql}
          table (assoc-in simple-schema [:properties 1 :type] :integer)
          rows `({:entity ~table :tuples ({:A 1 :B 2})})]
      (is (= `({:entity ~table :tuples ["INSERT INTO \"foo\".\"Example\" (\"A\",\"B\") VALUES (1,2);\n"]})
             (format-rows spec rows)))))

  (testing "SQL generation quotes and escapes string values."
    (let [spec {:format :sql}
          table (assoc-in simple-schema [:properties 1 :type] :string)
          rows `({:entity ~table
                  :tuples ({:A 1 :B  "Then O'Kelly came in with the \t hatchett."})})]
      (is (= `({:entity ~table
                :tuples ["INSERT INTO \"foo\".\"Example\" (\"A\",\"B\") VALUES (1,'Then O''Kelly came in with the \t hatchett.');\n"]})
             (format-rows spec rows)))))

  (testing "SQL generation flattens out embedded objects."
    (let [spec {:format :sql}
          data `({:entity ~person-schema :tuples ~(list {:Name "John Doe" :Address {:Address1 "123 Main" :City "Springfield"}})})]
      (is (= [{:entity person-schema :tuples ["INSERT INTO \"Person\" (\"Name\",\"Address.Address1\",\"Address.City\") VALUES ('John Doe','123 Main','Springfield');\n"]}]
             (format-rows spec data)))))

  (testing "SQL generation formats DATE values."
    (let [spec {:format :sql}
          table (assoc-in simple-schema [:properties 1 :type] :datetime)
          rows `({:entity ~table :tuples ({:A 1 :B ~(Date. 1109741401000)})})]
      (is (= `({:entity ~table :tuples ["INSERT INTO \"foo\".\"Example\" (\"A\",\"B\") VALUES (1,'2005-03-01T23:30:01.000-06:00');\n"]})
                  ; TODO: handle timezone in test
             (format-rows spec rows))))))

(deftest json-formatter-test
  (testing "Basic JSON generation."
    (let [spec {:format :json}
          table simple-schema
          rows `({:entity ~table :tuples ({:A 1 :B  "\"Thus sayeth...\""})})]
      (is (= `[{:entity ~table :tuples ["[{\"A\":1,\"B\":\"\\\"Thus sayeth...\\\"\"}]"]}]
             (format-rows spec rows))))))

(deftest xml-formatter-test
  (testing "Basic XML generation."
    (let [spec {:format :xml}
          table simple-schema
          rows `({:entity ~table :tuples ({:A 1 :B  "\"Thus sayeth...\""} {:A 2 :B "three"})})
          result (format-rows spec rows)]
      (is (= `[{:entity ~table :tuples ["<?xml version=\"1.0\" encoding=\"UTF-8\"?><Example><A>1</A><B>\"Thus sayeth...\"</B></Example>"
                                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Example><A>2</A><B>three</B></Example>"]}]
             result))))

  (testing "XML generation with embedded objects."
    (let [spec {:format :xml}
          person-table {:name "Person" :schema nil :properties [{:name "Address" :type :complex} {:name "Name" :type :string}]}
          data `({:entity ~person-table :tuples ~(list {:name "John Doe" :Address {:Address1 "123 Main" :City "Springfield"}})})]
      (is (= [{:entity person-table :tuples ["<?xml version=\"1.0\" encoding=\"UTF-8\"?><Person><name>John Doe</name><Address><Address1>123 Main</Address1><City>Springfield</City></Address></Person>"]}]
             (format-rows spec data)))))

  (testing "Basic XML generation with sequences."
    (let [spec {:format :xml}
          table (-> simple-schema
                    (assoc-in [:properties 1 :type] :sequence)
                    (assoc-in [:properties 1 :properties] [{:name "items" :type :integer}]))
          rows `({:entity ~table :tuples ({:A 1 :B  [1 2]} {:A 2 :B [3 5 6]})})
          result (format-rows spec rows)]
      (is (= `[{:entity ~table :tuples ["<?xml version=\"1.0\" encoding=\"UTF-8\"?><Example><A>1</A><B><item>1</item><item>2</item></B></Example>"
                                        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Example><A>2</A><B><item>3</item><item>5</item><item>6</item></B></Example>"]}]
             result)))))
