(ns blutwurst.planner
    (:require [taoensso.timbre :as timbre :refer [trace]]))

(defn create-data-generation-plan [schema]
 (trace schema)
 (:tables schema))
