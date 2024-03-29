(ns blutwurst.database-test
  (:require [clojure.test :refer :all]
            [blutwurst.database :refer :all]
            [blutwurst.logging-fixture :refer :all]
            [blutwurst.in-memory-db-utilities :refer :all]
            [clojure.data :refer :all]
            [clojure.pprint :refer :all]))

(use-fixtures :each db-fixture logging-fixture)

(def person-table {:name "PERSON",
                   :schema "DBO",
                   :dependencies []
                   :type :complex
                   :properties
                   [
                    {:name "BIRTHDATE", :type :date, :constraints {:nullable true}}
                    {:name "NAME", :type :string, :constraints {:nullable true :maximum-length 100}}
                    {:name "ID", :type :integer, :constraints {:minimum-value 0 :maximum-value 2147483647 :nullable false}}]})

(def full-expected-graph {:entities
                          [{:name "PURCHASETYPE" :schema "DBO" :type :complex
                            :properties [{:name "CATEGORY" :type :string  :constraints {:nullable true :maximum-length 50}}
                                         {:name "NAME" :type :string :constraints {:nullable true :maximum-length 50}}]
                            :dependencies []}
                           {:name "PURCHASE",
                            :schema "DBO",
                            :type :complex
                            :dependencies [{:dependency-name "CONSTRAINT_96"
                                            :target-name "PERSON"
                                            :target-schema "DBO"
                                            :links {"PURCHASEDBYID" "ID"}}
                                           {:dependency-name "CONSTRAINT_9"
                                            :target-name "PURCHASETYPE"
                                            :target-schema "DBO"
                                            :links {"PURCHASETYPENAME" "NAME"
                                                    "PURCHASETYPECATEGORY" "CATEGORY"}}]
                            :properties
                            [{:name "PURCHASEDBYID",
                              :type :integer,
                              :constraints {:minimum-value 0
                                            :maximum-value 2147483647,
                                            :nullable false}}
                             {:name "PURCHASETYPECATEGORY" :type :string :constraints {:nullable true :maximum-length 50}}
                             {:name "PURCHASETYPENAME" :type :string :constraints {:nullable true :maximum-length 50}}
                             {:name "AMOUNT", :type :decimal,  :constraints {:nullable true :maximum-value 999.99 :minimum-value -999.99}}
                             {:name "ID", :type :integer,  :constraints {:minimum-value 0 :maximum-value 2147483647 :nullable true}}]}
                           person-table]})

(deftest table-graph-tests
  (testing "Returns only specified tables when a list is provided."
    (let [spec {:connection-string connection-string :included-tables '("PERSON")}
          table-graph (retrieve-table-graph spec)]
      (is (= {:entities [person-table]} table-graph))))

  (testing "Connects to an in-memory database and returns basic table list."
    (let [spec {:connection-string connection-string :included-schemas '("DBO")} ; There was originally no included-schema, but this now returns system tables as well.
          table-graph (retrieve-table-graph spec)]
      (is (= full-expected-graph table-graph))))

  (testing "Table scans exclude schemas when at least one is provided."
    (let [spec {:connection-string connection-string :included-schemas '("FOOBAR")}
          table-graph (retrieve-table-graph spec)]
      (is (= {:entities []} table-graph))))

  (testing "Table scans include schemas when at least one is provided."
    (let [spec {:connection-string connection-string :included-schemas '("DBO")}
          table-graph (retrieve-table-graph spec)]
      (is (= full-expected-graph table-graph)))))

(deftest column-constraint-tests
   (testing "Integer columns should have a maximum value of 2^31 - 1."
      (let [spec {:connection-string connection-string :included-schemas '("DBO")}
          table-graph (retrieve-table-graph spec)]
      (is (= full-expected-graph table-graph)))))
