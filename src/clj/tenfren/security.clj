(ns tenfren.security
  "JWT, time based OTP, and general security functions."
  (:require
   [buddy.sign.jwt :as jwt]
   [one-time.core :as ot]
   [one-time.totp  :as totp]
   [tick.alpha.api :as tick]))

;; JWT

(defn create-jwt-token
  [{:keys [:jwt-secret :jwt-opts :jwt-token-expire-secs :sub]}
   {:keys [:user/id :user/email]}]
  (let [now (tick/now)
        exp (tick/>> now (tick/new-duration jwt-token-expire-secs :seconds))
        claims {:iss "tenfren.com"
                :iat now
                :exp exp
                :sub sub
                :user/id id
                :user/email email}]
    (jwt/sign claims jwt-secret jwt-opts)))

;; Time based One Time Password (TOTP)

(defn create-totp-token
  [key totp-step-secs]
  (totp/get-token key {:time-step totp-step-secs}))

(defn create-totp-token-details
  [key totp-step-secs]
  {:token (create-totp-token key totp-step-secs)
   :expires-in-secs totp-step-secs})

(defn valid-totp-token?
  [token key totp-step-secs]
  (ot/is-valid-totp-token? token key {:time-step totp-step-secs}))

(defn generate-otp-secret-key
  []
  (ot/generate-secret-key))

;; General

(defn registered? [user]
  (and user (:user/active user)))

(def unregistered?
  (complement registered?))

(defn admin? [user]
  (-> user :user/roles set (contains? "admin")))

(defn resource-owner? [resource-email user]
  (= (:user/email user) (:user/email resource-email)))

(defn has-permissions? [resource-email user]
  (or (admin? user)
      (resource-owner? resource-email user)))

(defn account-locked? [user now lockout-period-in-ms]
  (if-let [lockout-time (:user/lockout-time user)]
    (let [now-ms (inst-ms now)
          lockout-ms (inst-ms lockout-time)
          expiry-ms (+ lockout-ms lockout-period-in-ms)]
      (> expiry-ms  now-ms))
    false))

(comment
  (do
    (def secret-key (ot/generate-secret-key))
    (def token (ot/get-totp-token secret-key))
    (ot/is-valid-totp-token? token secret-key)))
