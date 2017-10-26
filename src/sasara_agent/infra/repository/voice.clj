(ns sasara-agent.infra.repository.voice
  (:require [clojure.spec.alpha :as s]
            [clojure.core.async :refer [go chan put! <!]]
            [cheshire.core :refer [generate-string]]
            [com.stuartsierra.component :as component]
            [sasara-agent.domain.entity.voice :as voice]
            [sasara-agent.infra.datasource.pubsub :as pubsub]
            [sasara-agent.infra.datasource.cloud-storage :as cloud-storage]
            [sasara-agent.infra.datasource.shell :as shell]))

(s/def ::voice-repository-component
  (s/keys :opt-un [:pubsub/pubsub-publisher-component
                   :cloud-storage/cloud-storage-component
                   :shell/shell-component]))

(def topic-key :sasara-voice)

(s/fdef upload-voice
  :args (s/cat :comp ::voice-repository-component
               :voice ::voice/voice)
  :ret string?)
(defn- upload-voice
  [{:keys [cloud-storage-component] :as comp} voice]
  (cloud-storage/upload-file cloud-storage-component
                             {:file-name (-> voice :content hash str)
                              :file (-> voice :content)}))

(s/def ::message string?)
(s/def ::link string?)
(s/def ::voice-message
  (s/keys :req-un [::link ::message]))

(s/fdef ->voice-message
  :args (s/cat :voice ::voice/voice
               :link ::link)
  :ret ::voice-message)
(defn- ->voice-message
  [voice link]
  {:message (:message voice)
   :link link})

(s/fdef publish-voice
  :args (s/cat :comp ::voice-repository-component
               :voice ::voice/voice
               :link ::link)
  :ret #(instance? (-> (chan) class) %))
(defn- publish-voice
  [{:keys [pubsub-publisher-component] :as comp} voice link]
  (let [c (chan)]
    (pubsub/publish pubsub-publisher-component
                        topic-key
                        (-> (->voice-message voice link) generate-string)
                        #(put! c %)
                        #(put! c (ex-info "Publish failed"
                                          {:voice voice
                                           :link link
                                           :error-message (.getMessage %)})))
    c))

(s/fdef publish
  :args (s/cat :comp ::voice-repository-component
               :voice ::voice/voice)
  :ret #(instance? (-> (chan) class) %))
(defn publish
  [comp voice]
  (->> (upload-voice comp voice)
       (publish-voice comp voice)))

(defrecord VoiceRepositoryComponent [pubsub-publisher]
  component/Lifecycle
  (start [this]
    (println ";; Starting VoiceRepositoryComponent")
    (-> this
        (update :pubsub-publisher-component #(pubsub/create-publisher % topic-key))))
  (stop [this]
    (println ";; Stoppoing VoiceRepositoryComponent")
    (-> this
        (dissoc :pubsub-publisher-component))))

(defn voice-repository-component []
  (map->VoiceRepositoryComponent {}))
