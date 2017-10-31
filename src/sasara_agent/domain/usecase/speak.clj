(ns sasara-agent.domain.usecase.speak
  (:require [com.stuartsierra.component :as component]
            [clojure.spec.alpha :as s]
            [clojure.core.async :refer [chan put! close!]]
            [sasara-agent.domain.entity.intent :as intent]
            [sasara-agent.domain.entity.voice :as voice]
            [sasara-agent.infra.repository.intent :as intent-repo]
            [sasara-agent.infra.repository.voice :as voice-repo]))

(s/def ::channel #(instance? (-> (chan) class) %))
(s/def ::speak-usecase-component
  (s/keys :req-un [:intent-repo/intent-repository-component
                   :voice-repo/voice-repository-component]))

(s/fdef subscribe-intent
  :args (s/cat :c ::speak-usecase-component)
  :ret ::channel)
(defn subscribe-intent
  [{:keys [intent-repository-component] :as c}]
  (intent-repo/subscribe-intent intent-repository-component))

(s/fdef response-voice
  :args (s/cat :c ::speak-usecase-component
               :intent ::intent/intent)
  :ret ::channel)
(defn response-voice
  [{:keys [intent-repository-component voice-repository-component] :as c}
   intent]
  (->> (intent-repo/intent->voice
         intent-repository-component
         intent)
       (voice-repo/publish voice-repository-component)))

(defrecord SpeakUsecaseComponent []
  component/Lifecycle
  (start [this]
    (println ";; Starting SpeakUsecaseComponent")
    this)
  (stop [this]
    (println ";; Stopping SpeakUsecaseComponent")
    this))

(defn speak-usecase-component []
  (map->SpeakUsecaseComponent {}))
