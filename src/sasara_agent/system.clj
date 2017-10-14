(ns sasara-agent.system
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [sasara-agent.app.agent :refer [app-agent-component]]
            [sasara-agent.infra.datasource.cloud-storage :refer [cloud-storage-component]]
            [sasara-agent.infra.datasource.shell :refer [shell-component]]
            [sasara-agent.infra.repository.example :refer [example-repository-component]]
            [sasara-agent.domain.usecase.example :refer [example-usecase-component]])
  (:gen-class))

(defn sasara-agent-system
  [{:keys [sasara-agent-bucket-name
           sasara-agent-command-path] :as conf}]
  (component/system-map
    :cloud-storage-datasource (cloud-storage-component sasara-agent-bucket-name)
    :shell-datasource (shell-component sasara-agent-command-path)))

(defn load-config []
  {:sasara-agent-bucket-name (env :sasara-agent-bucket-name)
   :sasara-agent-command-path (env :sasara-agent-command-path)})

(defn -main []
  (component/start
    (sasara-agent-system (load-config))))
