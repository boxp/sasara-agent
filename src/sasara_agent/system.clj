(ns sasara-agent.system
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [sasara-agent.app.agent :refer [app-agent-component]]
            [sasara-agent.infra.datasource.cloud-storage :refer [cloud-storage-component]]
            [sasara-agent.infra.datasource.shell :refer [shell-component]]
            [sasara-agent.infra.datasource.pubsub :refer [pubsub-publisher-component pubsub-subscription-component]]
            [sasara-agent.infra.repository.voice :refer [voice-repository-component]]
            [sasara-agent.infra.repository.intent :refer [intent-repository-component]]
            [sasara-agent.domain.usecase.speak :refer [speak-usecase-component]]
            [sasara-agent.app.agent :refer [app-agent-component]])
  (:gen-class))

(defn sasara-agent-system
  [{:keys [sasara-agent-bucket-name
           sasara-agent-command-path] :as conf}]
  (component/system-map
    :cloud-storage-component (cloud-storage-component sasara-agent-bucket-name)
    :shell-component (shell-component sasara-agent-command-path)
    :pubsub-publisher-component (pubsub-publisher-component)
    :pubsub-subscription-component (pubsub-subscription-component)
    :voice-repository-component (component/using (voice-repository-component)
                                                 [:cloud-storage-component
                                                  :pubsub-publisher-component])
    :intent-repository-component (component/using (intent-repository-component)
                                                  [:pubsub-subscription-component])
    :speak-usecase-component (component/using (speak-usecase-component)
                                                [:intent-repository-component
                                                 :voice-repository-component])
    :app-agent-component (component/using (app-agent-component)
                                          [:speak-usecase-component])))

(defn load-config []
  {:sasara-agent-bucket-name (env :sasara-agent-bucket-name)
   :sasara-agent-command-path (env :sasara-agent-command-path)})

(defn -main []
  (component/start
    (sasara-agent-system (load-config))))
