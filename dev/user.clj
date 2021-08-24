(ns user
  (:require
   [integrant.repl :as ig-repl]
   [integrant.repl.state :as state]
   [tenfren.config :as tc]
   [tenfren.system]))

(ig-repl/set-prep! (fn [] tc/default-config))

(def go ig-repl/go)
(def halt ig-repl/halt)
(def reset ig-repl/reset)
(def reset-all ig-repl/reset-all)
(defn system [] (or state/system (throw (ex-info "System not running" {}))))

(comment
  (go)
  (halt)
  (reset)
  (reset-all)
  (system))
