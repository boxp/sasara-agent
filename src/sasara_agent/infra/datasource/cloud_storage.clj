(ns sasara-agent.infra.datasource.cloud-storage
  (:import (java.io InputStream)
           (com.google.auth.oauth2 ServiceAccountCredentials)
           (com.google.cloud.storage Acl Acl$User Acl$Role Storage StorageOptions Storage$BucketGetOption Storage$BlobTargetOption Bucket BucketInfo BlobInfo))
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [com.stuartsierra.component :as component]))

(s/def ::bucket-name string?)
(s/def ::file-name string?)
(s/def ::storage
  (s/with-gen (s/nilable #(instance? Storage %))
    (fn [] (gen/fmap (fn [_] (-> (StorageOptions/getDefaultInstance) .getService))
                     (s/gen int?)))))
(s/def ::file
  (s/with-gen #(instance? (-> "" .getBytes class) %)
    (fn [] (gen/fmap (fn [s] (-> s .getBytes))
                     (s/gen string?)))))
(s/def ::cloud-storage-component
  (s/keys :req-un [::bucket-name ::storage]))

(s/def ::upload-file-opts
  (s/keys :req-un [::file ::file-name]))
(s/fdef upload-file
  :args (s/cat :c ::cloud-storage-component
               :opts ::upload-file-opts)
  :ret string?)
(defn upload-file
  [{:keys [storage bucket-name] :as c}
   {:keys [file file-name] :as opts}]
  (-> (.create storage
        (-> (BlobInfo/newBuilder bucket-name file-name)
            (.setAcl [(Acl/of (Acl$User/ofAllUsers) Acl$Role/READER)])
            .build)
        file
        (into-array Storage$BlobTargetOption []))
      .getMediaLink))

(s/fdef get-storage
  :args (s/cat)
  :ret ::storage)
(defn get-storage []
  (-> (StorageOptions/getDefaultInstance)
      .getService))

(defrecord CloudStorageComponent [bucket-name storage]
  component/Lifecycle
  (start [this]
    (println ";; Starting CloudStorageComponent")
    (-> this
        (assoc :storage (get-storage))))
  (stop [this]
    (println ";; Stopping CloudStorageComponent")
    (-> this
        (dissoc :storage))))

(s/fdef cloud-storage-component
  :args (s/cat :bucket-name ::bucket-name)
  :ret ::cloud-storage-component)
(defn cloud-storage-component
  [bucket-name]
  (map->CloudStorageComponent {:bucket-name bucket-name}))
