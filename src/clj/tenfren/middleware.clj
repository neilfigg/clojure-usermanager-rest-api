(ns tenfren.middleware
  "Provides the following additional functionality to `handler`:
  - authentication and authorization
  - exception handling
  - adds the env configuration to the request so fns
  can pull out db connection pool, auth config, and notifier config."
  (:require
   [buddy.auth :as buddy-auth]
   [buddy.auth.backends :as buddy-auth-backends]
   [buddy.auth.middleware :as buddy-auth-middleware]
   [buddy.hashers :as buddy-hashers]
   [clojure.tools.logging :as log]
   [malli.error :as me]
   [reitit.coercion :as coercion]
   [reitit.ring.middleware.exception :as exception]
   [tenfren.core :as t-core]
   [tenfren.security :as t-security]
   [tick.alpha.api :as tick]))

(defn token-auth-middleware
  "JWT token authentication.
  If the token is valid, adds the User `:identity` to the request."
  [handler]
  (fn [req]
    (let [jwt-secret (-> req :env :auth :jwt-secret)
          jwt-opts (-> req :env :auth :jwt-opts)
          token-backend (buddy-auth-backends/jws {:secret jwt-secret
                                                  :options jwt-opts})]
      ((buddy-auth-middleware/wrap-authentication handler token-backend) req))))

(defn otp-middleware
  "Check the time based one time password (TOTP) is valid."
  [handler]
  (fn [request]
    (let [db (-> request :env :db)
          totp-step-secs (-> request :env :auth :totp-step-secs)
          email (->> request :parameters :body :email (assoc {} :user/email))
          token (-> request :parameters :body :verification-code)
          user (t-core/find-user-by-email db email)
          token-key (:user/token-key user)]
      (if (and (seq token-key)
               (t-security/valid-totp-token? token token-key totp-step-secs))
        (handler (assoc request :identity user))
        (do (log/info "opt-middleware failure:" {:email email :token token :user user})
            {:status 401 :body {:error "Invalid Verification Code"}})))))

(defn admin-middleware
  "Check the User is an Admin."
  [handler]
  (fn [request]
    (let [db (-> request :env :db)
          identity (-> request :identity)
          user (t-core/find-user-by-email db identity)]
      (if (t-security/admin? user)
        (handler request)
        (do (log/info "admin-middleware failure:" {:identity user :user user})
            {:status 403 :body {:error "Admin role required"}})))))

(defn permissions-middleware
  "Check that the User is an Admin, or they own the resource identified by `:user/email`."
  [handler]
  (fn [request]
    (let [db (-> request :env :db)
          identity (-> request :identity)
          email (-> request :parameters :path :email)
          user (t-core/find-user-by-email db identity)
          resource-email {:user/email email}]
      (if (t-security/has-permissions? resource-email user)
        (handler request)
        (do (log/info "permissions-middleware failure:" {:identity user :user user :resource-email email})
            {:status 403 :body {:error "Only an Admin or the owning User has permissions."}})))))

(defn auth-middleware
  "Middleware used in routes that require authentication.
  Buddy checks if request key `:identity` is set to a truthy value by any previous middleware."
  [handler]
  (fn [request]
    (if (buddy-auth/authenticated? request)
      (handler request)
      {:status 401 :body {:error "Unauthorized"}})))

(defn basic-auth
  "Authentication function called from basic-auth middleware for each
  request. The result of this function will be added to the request
  under key `:identity`."
  [{{:keys [db auth]} :env} {:keys [username password]}]
  (log/info "login:" {:username username :password "[REDACTED]"})
  (let [user (t-core/find-user-by-email db {:user/email username})
        max-authentication-attempts (:max-authentication-attempts auth)
        authentication-lockout-ms (:authentication-lockout-ms auth)]
    (cond
      (t-security/unregistered? user)
      false
      (t-security/account-locked? user (tick/now) authentication-lockout-ms)
      (throw (ex-info "Account is locked due to too many failed login attempts. Try again in 15 minutes."
                      {:type :account-locked}))
      (buddy-hashers/check password (:user/password user))
      (assoc user :user/token (t-security/create-jwt-token auth user))
      :else (do
              (t-core/unsuccessful-login db user max-authentication-attempts authentication-lockout-ms)
              false))))

(defn basic-auth-middleware
  "Authenticates requests using http-basic authentication."
  [handler]
  (let [auth-backend (buddy-auth-backends/basic
                      {:authfn (partial basic-auth)
                       :realm "tenfren"})]
    (buddy-auth-middleware/wrap-authentication handler auth-backend)))

(def env-middleware
  "Adds the environment to the request. The env keys are `:db` `:notifier` `:auth`.
  This is an example of reitit compiled middleware. Also useful for debugging
  as the middleware `:name` is logged."
  {:name ::env
   :compile (fn [{:keys [env]} _]
              (fn [handler]
                (fn [req]
                  (handler (assoc req :env env)))))})

(defn cors-middleware
  "Cross-origin Resource Sharing (CORS) middleware.
  Allow requests from all origins, all http methods and Authorization and Content-Type headers."
  [handler]
  (fn [request]
    (let [allowed-origin (-> request :env :auth :cors-allowed-origins)
          allowed-methods (-> request :env :auth :cors-allowed-methods)
          allowed-headers (-> request :env :auth :cors-allowed-headers)
          max-age (-> request :env :auth :cors-max-age)
          resp (handler request)]
      (-> resp
          (assoc-in [:headers "Access-Control-Allow-Origin"] allowed-origin)
          (assoc-in [:headers "Access-Control-Allow-Methods"] allowed-methods)
          (assoc-in [:headers "Access-Control-Allow-Headers"] allowed-headers)
          (assoc-in [:headers "Access-Control-Max-Age"] max-age)))))

;; exceptions

(defn- coercion-error-handler-400 []
  (fn [e _]
    {:status 400
     :body   {:message (str (me/humanize (ex-data e)))
              :type :request-coercion-error}}))

(defn- coercion-error-handler-500 []
  (fn [_ _]
    {:status 500
     :body   {:message "Something has gone wrong on our end. Please try again."
              :type :response-coercion-error}}))

(defn- error-handler-500 []
  (fn [_ _]
    {:status 500
     :body   {:message "Something has gone wrong on our end. Please try again."
              :type :unknown-error}}))

(defn- default-error-handler [status type]
  (fn [e _]
    {:status status
     :body   {:message (ex-message e)
              :type type}}))

(def exception-handlers
  {:invalid-token          (default-error-handler 401 :invalid-token)
   :invalid-security-code  (default-error-handler 401 :invalid-security-code)
   :account-locked         (default-error-handler 403 :account-locked)
   :no-permission          (default-error-handler 403 :no-permission)
   :password-mismatch      (default-error-handler 403 :password-mismatch)
   :user-not-found         (default-error-handler 404 :user-not-found)
   :email-not-found        (default-error-handler 404 :email-not-found)
   :reminder-not-found     (default-error-handler 404 :reminder-not-found)
   :registration-conflict  (default-error-handler 409 :registration-conflict)
   :email-conflict         (default-error-handler 409 :email-conflict)})

(def exception-middleware
  (exception/create-exception-middleware
   (merge
    exception/default-handlers
    exception-handlers
    {::coercion/request-coercion (coercion-error-handler-400)
     ::coercion/reponse-coercion (coercion-error-handler-500)
     ::exception/default (error-handler-500)
     ::exception/wrap (fn [handler e request]
                        (log/error e (ex-message e))
                        (handler e request))})))
