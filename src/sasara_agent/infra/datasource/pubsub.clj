(ns sasara-agent.infra.datasource.pubsub
  (:import (com.google.protobuf ByteString)
           (com.google.common.util.concurrent MoreExecutors)
           (com.google.api.core ApiFutures
                                ApiFutureCallback
                                ApiService$Listener)
           (com.google.cloud ServiceOptions)
           (com.google.cloud.pubsub.v1 TopicAdminClient
                                           Publisher
                                           SubscriptionAdminClient
                                           Subscriber
                                           MessageReceiver)
           (com.google.pubsub.v1 Topic
                                 TopicName
                                 SubscriptionName
                                 Subscription
                                 PubsubMessage
                                 PushConfig))
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [com.stuartsierra.component :as component]))

(s/def ::topic-key keyword?)
(s/def ::subscription-key keyword?)
(s/def ::project-id (s/nilable string?))
(s/def ::publisher
  (s/with-gen #(instance? Publisher %)
    (fn [] (gen/fmap (fn [s] (Publisher/defaultBuilder
                               (TopicName/create s s)))
                     (s/gen string?)))))
(s/def ::publishers
  (s/map-of keyword? ::publisher))
(s/def ::subscriber
  (s/with-gen #(instance? Subscriber %)
    (fn [] (gen/fmap (fn [s] (Subscriber/defaultBuilder
                               (SubscriptionName/create s s)
                               (reify MessageReceiver
                                 (receiveMessage [this message consumer]))))
                     (s/gen string?)))))
(s/def ::subscribers
  (s/map-of keyword? ::subscriber))
(s/def ::subscription #(instance? Subscription %))
(s/def ::pubsub-message
  (s/with-gen #(instance? PubsubMessage %)
    (fn [] (gen/fmap (fn [s] (-> s .getBytes PubsubMessage/parseFrom))
                     (s/gen string?)))))
(s/def ::pubsub-publisher-component
  (s/keys :req-un [::project-id ::publishers]))
(s/def ::pubsub-subscriber-component
  (s/keys :req-un [::project-id ::subscribers]))

(s/fdef create-topic
  :args (s/cat :comp ::pubsub-publisher-component
               :topic-key ::topic-key)
  :ret #(instance? Topic %))
(defn create-topic
  [comp topic-key]
  (let [topic-admin-cli (TopicAdminClient/create)]
    (try
        (->> (TopicName/create (:project-id comp)
                               (name topic-key))
             (.createTopic topic-admin-cli))
        (catch Exception e
          (TopicName/create (:project-id comp)
            (name topic-key))))))

(s/fdef create-publisher
  :args (s/cat :comp ::pubsub-publisher-component
               :topic-key ::topic-key)
  :ret ::pubsub-publisher-component)
(defn create-publisher
  [comp topic-key]
  (if-not (get (:publishers comp) topic-key)
    (let [topic-name (create-topic comp topic-key)]
          (->> topic-name
               Publisher/defaultBuilder
               .build
               (assoc-in comp [:publishers topic-key])))
    comp))

(s/fdef ::on-success
        :args (s/cat :result string?)
        :ret true?)
(s/fdef publish
  :args (s/cat :comp ::pubsub-publisher-component
               :topic-key ::topic-key
               :message string?
               :on-success ::on-success
               :on-failure #(fn? %))
  :ret ::pubsub-publisher-component)
(defn publish
  [{:keys [publishers project-id] :as comp} topic-key message on-success on-failure]
  (let [data (ByteString/copyFromUtf8 message)
        pubsub-message (-> (PubsubMessage/newBuilder) (.setData data) .build)
        publisher (-> publishers topic-key)
        message-id-future (.publish publisher pubsub-message)
        callback (reify ApiFutureCallback
                   (onSuccess [this message-id]  #(on-success message-id))
                   (onFailure [this e] #(on-failure e)))]
    (ApiFutures/addCallback message-id-future callback)))

(defrecord PubSubPublisherComponent [project-id publishers]
  component/Lifecycle
  (start [this]
    (println ";; Starting PubSubPublisherComponent")
    (-> this
        (assoc :project-id (ServiceOptions/getDefaultProjectId))))
  (stop [this]
    (println ";; Stopping PubSubPublisherComponent")
    (-> this
        (dissoc :publishers)
        (dissoc :project-id))))

(s/fdef pubsub-publisher-component
  :args (s/cat)
  :ret ::pubsub-publisher-component)
(defn pubsub-publisher-component
  []
  (map->PubSubPublisherComponent {}))

(s/fdef create-subscription
  :args (s/cat :comp ::pubsub-subscriber-component
               :topic-key ::topic-key
               :subscription-key ::subscription-key)
  :ret ::subscription)
(defn create-subscription
  [comp topic-key subscription-key]
  (let [topic-name (create-topic comp topic-key)
        subscription-name (SubscriptionName/create (:project-id comp) (name subscription-key))
        push-config (-> (PushConfig/newBuilder) .build)
        ack-deadline-second 0]
    (-> (SubscriptionAdminClient/create)
        (.createSubscription subscription-name
                             topic-name
                             push-config
                             ack-deadline-second))))

(s/fdef add-subscriber
  :args (s/cat :comp ::pubsub-subscriber-component
               :topic-key ::topic-key
               :subscription-key ::subscription-key
               :on-receive (s/fspec :args (s/cat :message ::pubsub-message)
                                    :ret nil?))
  :ret ::pubsub-subscriber-component)
(defn add-subscriber
  [comp topic-key subscription-key on-receive]
  (let [subscription-name (SubscriptionName/create (:project-id comp) (name subscription-key))
        receiver (reify MessageReceiver
                   (receiveMessage [this message consumer]
                     (on-receive message)
                     (.ack consumer)))
        listener (proxy [ApiService$Listener] []
                   (failed [from failure]))
        subscriber (-> (Subscriber/defaultBuilder subscription-name receiver) .build)]
    (.addListener subscriber listener (MoreExecutors/directExecutor))
    (-> subscriber .startAsync .awaitRunning)
    (-> comp
        (assoc-in [:subscribers subscription-key] subscriber))))

(defrecord PubSubSubscriptionComponent [project-id subscribers]
  component/Lifecycle
  (start [this]
    (println ";; Starting PubSubSubscriptionComponent")
    (-> this
        (assoc :project-id (ServiceOptions/getDefaultProjectId))))
  (stop [this]
    (println ";; Stopping PubSubSubscriptionComponent")
    (doall (map #(.stopAsync %) (:subscribers this)))
    (-> this
        (dissoc :project-id)
        (dissoc :subscribers))))

(s/fdef pubsub-subscription-component
  :args (s/cat)
  :ret ::pubsub-subscriber-component)
(defn pubsub-subscription-component
  []
  (map->PubSubSubscriptionComponent {}))
