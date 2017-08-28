(ns blutwurst.jsonschema
 (:require [cheshire.core :as json]
           [clojure.java.io :as io]
           [taoensso.timbre :as timbre :refer [trace]]))

(defn parse-json-schema 
 "Accepts location, which should be coercible to a reader via clojure.java.io.reader,
  and returns an object formatted like a schema description used throughout Blutwurst."
 [location] 
 (with-open [r (io/reader location)]
  (let [json-document (json/parse-stream r)]
   json-document)))

(defn parse-json-schema-from-spec [spec] 
 (-> spec
     :connection-string
     parse-json-schema))
