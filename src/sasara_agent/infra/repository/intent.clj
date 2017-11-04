(ns sasara-agent.infra.repository.intent
  (:import (com.google.pubsub.v1 PubsubMessage))
  (:require [clojure.spec.alpha :as s]
            [clojure.core.async :refer [chan put! close!]]
            [cheshire.core :refer [parse-string]]
            [com.stuartsierra.component :as component]
            [sasara-agent.domain.entity.voice :as voice]
            [sasara-agent.domain.entity.intent :as intent]
            [sasara-agent.infra.datasource.pubsub :as pubsub]
            [sasara-agent.infra.datasource.shell :as shell]))

(s/def ::message #(instance? PubsubMessage %))
(s/def ::channel #(instance? (-> (chan) class) %))
(s/def ::intent-repository-component
  (s/keys :req-un [:pubsub/pubsub-subscription-component
                   :shell/shell-component
                   ::channel]))

(def topic-key :sasara-intent)
(def subscription-key :sasara-intent-agent)

(def voice-file-path "C:/WINDOWS/TEMP/sasara_output.wav")

(s/fdef message->intent
  :args (s/cat :message ::message)
  :ret ::intent/intent)
(defn message->intent
  [message]
  (-> message
      .getData
      .toStringUtf8
      (parse-string true)))

(s/fdef subscribe-intent
  :args (s/cat :comp ::intent-repository-component)
  :ret ::channel)
(defn subscribe-intent
  [comp]
  (:channel comp))

(s/fdef speak-intent
  :args (s/cat :comp ::shell/shell-component
               :intent ::intent/intent)
  :ret nil?)
(defn- speak-intent
  [{:keys [shell-component] :as comp} intent]
  (shell/exec-command shell-component {:text (:message intent)}))

(s/fdef fetch-voice
  :args (s/cat :intent ::intent/intent)
  :ret ::voice/voice)
(defn- fetch-voice
  [intent]
  (let [f (java.io.File. voice-file-path)
        ary (byte-array (.length f))
        is (java.io.FileInputStream. f)]
    (.read is ary)
    (.close is)
    {:content ary
     :message (:message intent)}))

(s/fdef intent->voice
  :args (s/cat :comp ::intent-repository-component
               :intent ::intent/intent)
  :ret ::voice/voice)
(defn intent->voice
  [comp intent]
  (do (speak-intent comp intent)
      (fetch-voice intent)))

(defrecord IntentRepositoryComponent [channel]
  component/Lifecycle
  (start [this]
    (let [c (chan 1 (map message->intent))]
      (println ";; Starting IntentRepositoryComponent")
      (try
        (pubsub/create-subscription (:pubsub-subscription-component this)
                                    topic-key
                                    subscription-key)
        (catch Exception e
          (println "Info: Already" topic-key "has exists")))
      (-> this
          (update :pubsub-subscription-component
                  #(pubsub/add-subscriber % topic-key subscription-key
                                          (fn [m]
                                            (put! c m))))
          (assoc :channel c))))
  (stop [this]
    (println ";; Stopping IntentRepositoryComponent")
    (when (:channel this) (close! (:channel this)))
    (-> this
        (dissoc :channel))))

(defn intent-repository-component []
  (map->IntentRepositoryComponent {}))
