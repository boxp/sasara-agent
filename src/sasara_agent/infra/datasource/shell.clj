(ns sasara-agent.infra.datasource.shell
  (:require [clojure.java.shell :as sh]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [com.stuartsierra.component :as component]))

(s/def ::text string?)
(s/def ::command-path string?)

(s/def ::shell-component
  (s/keys :req-un [::command-path]))

(s/def ::exec-command-opts
  (s/keys :req-un [::text]))
(s/fdef exec-command
  :args (s/cat :c ::shell-component
               :opts ::exec-command-opts)
  :ret nil?)
(defn exec-command
  [{:keys [command-path] :as c}
   {:keys [text] :as opts}]
  (let [result (sh/sh command-path text)]
    (if (not= 0 (:exit result))
      (throw
        (ex-info "Command execution failed"
          {:command-path command-path
           :error-message (:error result)})))))

(defrecord ShellComponent [command-path]
  component/Lifecycle
  (start [this]
    (println ";; Starting ShellComponent")
    this)
  (stop [this]
    (println ";; Stopping ShellComponent")
    this))

(s/fdef shell-component
  :args (s/cat :command-path ::command-path)
  :ret ::shell-component)
(defn shell-component
  [command-path]
  (map->ShellComponent {:command-path command-path}))
