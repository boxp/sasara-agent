(ns sasara-agent.infra.datasource.cloud-storage
  (:import (com.google.auth.oauth2 ServiceAccountCredentials)
           (com.google.cloud.storage Bucket BucketInfo)
           (com.google.cloud.storage Storage StorageOptions Storage$BucketGetOption))
  (:require [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]))

(s/def ::bucket-name string?)
(s/def ::storage #(instance? Storage %))
(s/def ::cloud-storage-component
  (s/keys :req [::bucket-name]
          :opt [::storage]))

(s/fdef upload-file
  :args (s/cat :c ::cloud-storage-component)
  :ret nil?)
(defn upload-file
  [{:keys [storage bucket-name] :as c}])

(s/fdef get-storage
  :ret ::storage)
(defn get-storage []
  (-> (StorageOptions/getDefaultInstance)
      .getService))

(defrecord CloudStorageComponent [bucket-name storage]
  component/Lifecycle
  (start [this]
    (println ";; Starting PubSubSubscriptionComponent")
    (-> this
        (assoc :storage (get-storage))))
  (stop [this]
    (println ";; Stopping PubSubSubscriptionComponent")
    (-> this
        (dissoc :storage))))

(defn cloud-storage-component
  [bucket-name]
  (map->CloudStorageComponent {:bucket-name bucket-name}))
