(ns tenfren.test-fixtures
  (:require
   [buddy.hashers :as hashers]
   [clojure.pprint :as p]
   [clojure.tools.logging :as log]
   [malli.generator :as mg]
   [next.jdbc :as jdbc]
   [tenfren.config :as t-config]
   [tenfren.core :as t-core]
   [tenfren.db :as t-db]
   [tenfren.schema :as t-schema]
   [tenfren.security :as t-security]
   [tenfren.system :as system]))

;;; system
(def config  t-config/default-config)
(def system (system/start-system! config))
(def db (-> system
            :tenfren/db
            t-db/default-ds-options))
(def auth (-> system
              :tenfren/auth))
(def handler (:tenfren/app system))

(log/info "*** Tests started with the following config ***")
(log/info (p/pprint  config))

;;; fixtures

(defn init-db [f]
  (t-db/create-db db)
  (f))

;;; security

(def max-authentication-attempts
  (:max-authentication-attempts auth))

(defn create-jwt-token
  [user]
  (t-security/create-jwt-token auth user))

(defn create-totp-token
  [key]
  (t-security/create-totp-token key (:totp-step-secs auth)))

;;; user

(defn gen-user
  "Generate a new User that is optionally persisted to the DB."
  ([]
   (gen-user {:schema t-schema/new-user
              :roles []
              :active? false
              :db? false
              :failed-login-attempts 0
              :lockout-time nil}))
  ([{:keys [schema roles active? db? failed-login-attempts lockout-time]
     :or   {schema t-schema/new-user
            roles t-core/default-roles
            active? false
            db? false
            failed-login-attempts 0
            lockout-time nil}}]
   (let [save-user (fn [db user]
                     (jdbc/with-transaction [tx db]
                       (let [tx-opts (t-db/default-ds-options tx)]
                         (->> (update user :user/password hashers/encrypt)
                              (t-db/save-user tx-opts)))))
         user (-> (mg/generate schema)
                  (assoc :user/active active?
                         :user/token-key (t-security/generate-otp-secret-key)
                         :user/roles roles
                         :user/failed-login-attempts failed-login-attempts
                         :user/lockout-time lockout-time
                         :user/created-timestamp nil
                         :user/modified-user nil
                         :user/modified-timestamp nil)
                  (dissoc :user/id
                          :user/token
                          :user/created-timestamp
                          :user/modified-timestamp))
         password (:user/password user)
         user (-> (if db? (save-user db user) user)
                  (assoc :user/password password))]
     user)))

(comment
  ;; clj -M:test
  (gen-user)
  (gen-user {:db? true})
  (gen-user {:schema t-schema/new-user :db? true})
  (gen-user {:schema t-schema/new-user :roles ["admin" "user"] :active? true :db? true})
  (gen-user {:schema t-schema/new-user :roles ["user"] :active? true :db? true})
  (gen-user {:roles ["user"] :active? true :db? true}))
