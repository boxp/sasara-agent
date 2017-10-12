(ns sasara-agent.app.agent
  (:require [com.stuartsierra.component :as component]))

(defrecord AppAgentComponent []
  component/Lifecycle
  (start [this]
    (println ";; Starting AppAgentComponent")
    this)
  (stop [this]
    (println ";; Stopping AppAgentComponent")
    this))

(defn app-agent-component
  []
  (map->AppAgentComponent {}))
