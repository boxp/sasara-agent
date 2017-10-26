(ns sasara-agent.infra.repository.intent
  (:require [clojure.spec.alpha :as s]
            [clojure.core.async :refer [chan put! close!]]
            [cheshire.core :refer [parse-string]]
            [com.stuartsierra.component :as component]
            [sasara-agent.domain.entity.voice :as voice]
            [sasara-agent.domain.entity.intent :as intent]
            [sasara-agent.infra.datasource.pubsub :as pubsub]
            [sasara-agent.infra.datasource.shell :as shell]))

(s/def ::channel #(instance? (-> (chan) class) %))
(s/def ::intent-repository-component
  (s/keys :req-un [:pubsub/pubsub-subscription-component
                   :shell/shell-component
                   ::channel]))

(def topic-key :sasara-intent)
(def subscription-key :sasara-intent-agent)

(s/fdef message->intent
  :args (s/cat :message string?)
  :ret ::intent/intent)
(defn message->intent
  [message]
  (-> message
      (parse-string true)))

(s/fdef subscribe
  :args (s/cat :comp ::intent-repository-component)
  :ret ::channel)
(defn subscribe
  [comp]
  (:channel comp))

(defrecord IntentRepositoryComponent [channel]
  component/Lifecycle
  (start [this]
    (let [c (chan)]
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
                                            (put! c (message->intent m)))))
          (assoc :channel c))))
  (stop [this]
    (println ";; Stopping IntentRepositoryComponent")
    (close! (:channel this))
    (-> this
        (dissoc :channel))))

(defn intent-repository-component []
  (map->IntentRepositoryComponent {}))
