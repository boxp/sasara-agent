(ns sasara-agent.app.agent
  (:require [clojure.spec.alpha :as s]
            [clojure.core.async :refer [go-loop <! chan put! close!]]
            [com.stuartsierra.component :as component]
            [sasara-agent.domain.usecase.speak :as speak]))

(s/def ::app-agent-component
  (s/keys :req-un [:speak/speak-usecase-component]))

(s/fdef agent-loop
  :args (s/cat :c ::app-agent-component)
  :ret nil?)
(defn agent-loop
  [{:keys [speak-usecase-component] :as c}]
  (let [intent-chan (speak/subscribe-intent speak-usecase-component)]
    (go-loop [intent (<! intent-chan)]
      (when intent
        (speak/response-voice speak-usecase-component intent)
        (recur (<! intent-chan))))))

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
