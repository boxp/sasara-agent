(ns sasara-agent.system
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [sasara-agent.app.agent :refer [app-agent-component]]
            [sasara-agent.infra.datasource.example :refer [example-datasource-component]]
            [sasara-agent.infra.repository.example :refer [example-repository-component]]
            [sasara-agent.domain.usecase.example :refer [example-usecase-component]])
  (:gen-class))

(defn sasara-agent-system
  [{:keys [sasara-agent-example-port
           sasara-agent-my-webapp-port] :as conf}]
  (component/system-map
    :example-datasource (example-datasource-component sasara-agent-example-port)
    :example-repository (component/using
                          (example-repository-component)
                          [:example-datasource])
    :example-usecase (component/using
                       (example-usecase-component)
                       [:example-repository])))

(defn load-config []
  {:sasara-agent-example-port (-> (or (env :sasara-agent-example-port) "8000") Integer/parseInt)
   :sasara-agent-my-webapp-port (-> (or (env :sasara-agent-my-webapp-port) "8080") Integer/parseInt)})

(defn -main []
  (component/start
    (sasara-agent-system (load-config))))
