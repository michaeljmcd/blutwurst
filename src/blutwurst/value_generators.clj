(ns blutwurst.value-generators
  (:import (java.util Random)
           (java.time OffsetDateTime ZonedDateTime Instant)
           (java.lang Math Integer)
           (org.apache.commons.math3.random RandomDataGenerator)
           (com.thedeanda.lorem LoremIpsum)
           (com.mifmif.common.regex Generex))
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [taoensso.timbre :as timbre :refer [trace error]]))

(def ^{:private true} random (RandomDataGenerator.))

(defn- crop-to-column-size [column input-string]
  (if (or (nil? input-string) (empty? input-string))
    input-string
    (subs input-string 0 (min (count input-string) 
                              (or (:length column)
                                  (-> column :constraints :maximum-length)
                                  20)))))

(defn- create-regex-generator [generator-name regex]
  {:name generator-name
   :determiner #(do % nil)
   :generator #(let [g (Generex. regex)]
                 (crop-to-column-size % (.random g)))})

(defn random-integer-under-value [max-value]
  (let [random (Random.)]
    (mod (.nextLong random) max-value)))

(defn- random-decimal [min-value max-value]
    (.nextUniform random (double min-value) (double max-value)))

(defn random-string [max-length]
  (let [g (Generex. "([A-Za-z0-9'\" \\n\\t!@#$%&*()])+")]
    (.random g (max 0 (- max-length (* 0.5 max-length))) max-length)))

(defn- random-datetime []
 (let [system-offset (.getOffset (OffsetDateTime/now))
       minimum-time (.toEpochMilli (.toInstant (ZonedDateTime/parse "1800-01-01T00:00:00-06:00[America/Chicago]")))
       maximum-time (.toEpochMilli (.toInstant (ZonedDateTime/parse "2100-12-31T23:59:59-06:00[America/Chicago]")))]

   (ZonedDateTime/ofInstant (Instant/ofEpochMilli (.nextLong random minimum-time maximum-time)) system-offset)))

(defn- random-date []
 (.toLocalDate (random-datetime)))

(defn- column-is-string? [column]
  (= :string (:type column)))

(defn- make-resource-generator-fn [resource garbage-rows value-index]
  (let [values (drop garbage-rows (-> resource io/resource io/reader csv/read-csv))
        value-count (count values)]
    (trace values)
    (fn [c]
      (crop-to-column-size c (string/trimr (nth (nth values (random-integer-under-value value-count)) value-index))))))

(defn- list-contains-type [type-list column]
  (some #{(string/upper-case (:type column))} type-list))

(defn- calculate-maximum-word-count [column]
  (let [base-count (or (:length column) 255)]
    (min base-count 2000)))

(def value-generation-strategies
  ^{:private true}
  [{:name "State Abbreviation Selector (U.S. and Canada)"
    :determiner #(and (column-is-string? %) (string/includes? (:name %) "STATE"))
    :generator (make-resource-generator-fn "state_table.csv" 1 2)}
   {:name "State Name Selector (U.S. and Canada)"
    :determiner #(do % nil)
    :generator (make-resource-generator-fn "state_table.csv" 1 1)}
   {:name "Full Name Selector (U.S.)"
    :determiner #(and (column-is-string? %) (string/includes? (:name %) "FULL") (string/includes? (:name %) "NAME"))
    :generator (let [last-name-fn (make-resource-generator-fn "Names_2010Census_Top1000.csv" 3 0)
                     first-name-fn (make-resource-generator-fn "census-derived-all-first.csv" 0 0)]
                 #(crop-to-column-size % (str (last-name-fn %) " " (first-name-fn %))))}
   {:name "First Name Selector (U.S.)"
    :determiner #(and (column-is-string? %) (string/includes? (:name %) "FIRST"))
    :generator (make-resource-generator-fn "census-derived-all-first.csv" 0 0)}
   {:name "Last Name Selector (U.S.)"
    :determiner #(and (column-is-string? %) (string/includes? (:name %) "LAST"))
    :generator (make-resource-generator-fn "Names_2010Census_Top1000.csv" 3 0)}
   {:name "City Generator"
    :determiner #(and (column-is-string? %) (string/includes? (:name %) "CITY"))
    :generator (let [l (LoremIpsum.)] (fn [c] (crop-to-column-size c (.getCity l))))}
   {:name "Email Generator"
    :determiner #(and (column-is-string? %) (string/includes? (:name %) "EMAIL"))
    :generator (let [l (LoremIpsum.)] (fn [c] (crop-to-column-size c (.getEmail l))))}
   {:name "Phone Number Generator"
    :determiner #(and (column-is-string? %) (string/includes? (:name %) "PHONE"))
    :generator (let [l (LoremIpsum.)] (fn [c] (crop-to-column-size c (.getPhone l))))}
   {:name "Postal Code Generator"
    :determiner #(and (column-is-string? %) (string/includes? (:name %) "ZIP"))
    :generator (let [l (LoremIpsum.)] (fn [c] (crop-to-column-size c (.getZipCode l))))}
   {:name "URL Generator"
    :determiner #(and (column-is-string? %) (string/includes? (:name %) "URL"))
    :generator (let [l (LoremIpsum.)] (fn [c] (crop-to-column-size c (.getUrl l))))}
   {:name "Text Generator"
    :determiner column-is-string?
    :generator (let [l (LoremIpsum.)] 
                (fn [c] 
                 (crop-to-column-size c 
                                      (.getWords l 0 (calculate-maximum-word-count c)))
                    ))}
   {:name "String Generator"
    :determiner column-is-string?
    :generator #(random-string (or (min (:length %) 2000) 255))}
   {:name "Integer Generator"
    :determiner #(= (:type %) :integer)
    :generator #(random-integer-under-value (or (-> % :constraints :maximum-value) Integer/MAX_VALUE))}
   {:name "Decimal Generator"
    :determiner #(= (:type %) :decimal)
    :generator #(random-decimal (or (-> % :constraints :minimum-value) 0)
                                (or (-> % :constraints :maximum-value) 1))}
   {:name "Date / Timestamp Generator"
    :determiner #(= (:type %) :datetime)
    :generator (fn [c] (random-datetime))}
   {:name "Date Generator"
    :determiner #(= (:type %) :date)
    :generator (fn [c] (random-date))}
   {:name "Null Value Generator"
    :determiner #(do % nil)
    :generator #(do % nil)}])

(defn- accrete-regex-generators [spec generators]
  (concat (map #(create-regex-generator (:name %) (:regex %)) (:regex-generators spec))
          generators))

(def create-generators (memoize (fn [spec]
                                  (->> value-generation-strategies
                                       (accrete-regex-generators spec)))))

(defn retrieve-registered-generators [spec]
  (map :name (create-generators spec)))
