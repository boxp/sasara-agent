(ns sasara-agent.domain.entity.voice
  (:import (java.io InputStream))
  (:require [clojure.spec.alpha :as s]))

(s/def ::content #(instance? (-> "" .getBytes class) %))
(s/def ::message string?)
(s/def ::voice
  (s/keys :req-un [::message ::content]))
