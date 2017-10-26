(ns sasara-agent.domain.entity.intent
  (:require [clojure.spec.alpha :as s]))

(s/def ::message string?)
(s/def ::intent
  (s/keys :req-un [::message]))
