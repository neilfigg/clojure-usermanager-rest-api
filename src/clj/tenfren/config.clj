(ns tenfren.config
  (:require
   [config.core :refer [env]]
   [clojure.tools.logging :as log]
   [integrant.core :as ig]))

(defn load-config
  "As an alt to the `default-config` an edn file containing config can be used."
  [path]
  (try
    (log/info "Loading config at " path)
    (ig/read-string (slurp path))
    (catch Exception e
      (log/error "Problem loading config at " (pr-str path) " " e)
      nil)))

(def default-config
  {:tenfren/db
   {:jdbc-url           (:db-url env)
    :adapter            (:db-adapter env)
    :database-name      (:db-name env)
    :server-name        (:db-host env)
    :port-number        (:db-port env)
    :username           (:db-user env)
    :password           (:db-password env)
    :auto-commit        (or (:db-auto-commit env) false)
    :connection-timeout (or (:db-connection-timeout env) 30000)
    :validation-timeout (or (:db-validation-timeout env) 5000)
    :idle-timeout       (or (:db-idle-timeout env) 600000)
    :max-lifetime       (or (:db-max-lifetime env) 1800000)
    :minimum-idle       (or (:db-minimum-idle env) 3)
    :maximum-pool-size  (or (:db-maximum-pool-size env) 4)
    :pool-name          (or (:db-pool-name env) "db-pool")}
   :tenfren/auth
   {:jwt-secret             (:auth-jwt-secret env)
    :jwt-opts               (or (:auth-jwt-opts env) {:alg :hs512})
    :jwt-token-expire-secs  (or (:auth-jwt-token-expire-secs env) 31557600)
    :totp-step-secs         (or (:auth-totp-step-secs env) 6000)
    :max-authentication-attempts (or (:auth-max-authentication-attempts env) 5)
    :authentication-lockout-ms   (or (:auth-authentication-lockout-ms env) 900000)
    :cors-allowed-origins   (or (:auth-cors-allowed-origins env)  ".*")
    :cors-max-age           (or (:auth-cors-max-age env) "600")
    :cors-allowed-headers   (or (:auth-cors-allowed-headers env) "Authorization, Content-Type")
    :cors-allowed-methods   (or (:auth-cors-allowed-methods env) "GET, PUT, PATCH, POST, DELETE, OPTIONS")
    :max-attempts           (or (:auth-max-attempts env) 3)}
   :tenfren/notifier
   {:enabled  (:notifier-enabled env)
    :host     (:notifier-host env)
    :user     (:notifier-user env)
    :password (:notifier-password env)
    :from     (:notifier-from env)}
   :tenfren/app
   {:db       (ig/ref :tenfren/db)
    :notifier (ig/ref :tenfren/notifier)
    :auth     (ig/ref :tenfren/auth)
    :version  (or (:app-version env) {:date "2021-08-01-v1"})}
   :tenfren/server
   {:app      (ig/ref :tenfren/app)
    :port     (or (:server-port env) 8091)}})
