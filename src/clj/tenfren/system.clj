(ns tenfren.system
  "Configures and starts the system."
  (:require
   [clojure.pprint :as p]
   [clojure.tools.logging :as log]
   [hikari-cp.core :as hikari]
   [integrant.core :as ig]
   [next.jdbc.transaction]
   [ring.adapter.jetty :as jetty]
   [tenfren.config :as t-config]
   [tenfren.handler :as t-handler]
   [tenfren.notifier :as t-notifier])
  (:gen-class))

;; https://github.com/seancorfield/next-jdbc/blob/develop/doc/transactions.md#nesting-transactions
(binding [next.jdbc.transaction/*nested-tx* :prohibit])

(defmethod ig/init-key :tenfren/app [_ env]
  (t-handler/app env))

(defmethod ig/init-key :tenfren/db [_ db-spec]
  (hikari/make-datasource db-spec))

(defmethod ig/halt-key! :tenfren/db [_ pool]
  (hikari/close-datasource pool))

(defmethod ig/init-key :tenfren/notifier [_ config]
  (if (:enabled config)
    (t-notifier/->SMTPEmailer config)
    (t-notifier/->LogEmailer)))

(defmethod ig/init-key :tenfren/auth [_ config]
  config)

(defmethod ig/init-key :tenfren/server [_ {:keys [app port]}]
  (jetty/run-jetty app {:port port :join? false}))

(defmethod ig/halt-key! :tenfren/server [_ server]
  (.stop server))

(defn mask [_]
  "[REDACTED]")

(defn start-system!
  ([]
   (start-system! t-config/default-config))
  ([config]
   (let [system (ig/init config)]
     (log/info "*** System started with the following config ***")
     (log/info (p/pprint
                (-> config
                    (update-in [:tenfren/db :password] mask)
                    (update-in [:tenfren/auth :jwt-secret] mask)
                    (update-in [:tenfren/auth :jwt-opts] mask)
                    (update-in [:tenfren/notifier :password] mask)
                    (update-in [:tenfren/notifier :user] mask))))
     system)))

(defn stop-system! [system]
  (ig/halt! system))

(defn -main [& args]
  (if (seq args)
    (let [file-path (first args)]
      (start-system! (t-config/load-config file-path)))
    (start-system!)))

(comment
  (-main)
  (-main "/path/some-other-config.edn"))
