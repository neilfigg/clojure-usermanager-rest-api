(ns tenfren.handler
  "Defines a RESTful API for processing HTTP requests and responses.
  Additional functionality, e.g. Authentication, Authorization,
  and exception handling, is provided by `middleware`,
  and processing logic by `core`."
  (:require
   [malli.util :as mu]
   [muuntaja.core :as m]
   [muuntaja.middleware]
   [reitit.coercion.malli]
   [reitit.dev.pretty :as pretty]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.malli]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [ring.logger :as logger]
   [tenfren.core :as t-core]
   [tenfren.db :as t-db]
   [tenfren.middleware :as t-mw]
   [tenfren.schema :as t-schema]))

;; Helpers

(defn ->identity-keys [m]
  (select-keys m [:user/id :user/email]))

;; Routes

(def swagger-routes
  ["/swagger.json"
   {:get
    {:no-doc true
     :swagger {:info {:title "Tenfren User Management REST API."
                      :description "Provides a Clojure REST API for common User Management application services like
                                    Registration, Login, Password management, Role based Authorization,
                                    and User CRUD APIs."}
               :tags [{:name "System", :description "Info about the running system"}
                      {:name "Users", :description "User management"}]}
     :handler (swagger/create-swagger-handler)}}])

(def system-routes
  ["/system"
   {:swagger {:tags ["System"]}}
   ["/info" {:get
             {:handler (fn [req]
                         (let [tenfren-version (-> req :env :version)]
                           {:status 200 :body {:message
                                               {:health "OK"
                                                :version tenfren-version
                                                :system (str req)}}}))}}]])

(def user-routes
  ["/users"
   {:swagger {:tags ["Users"]}}
   [""
    {:get
     {:summary "Get all the Users - Requires Admin role."
      :description "Requires an Authentication header <type> <credentials>.
                   Where the type is `Token` and the credentials are a signed `JWT`.
                   e.g. Token YWxhZGRpbjpvcGVuc2VzYW1l..."
      :middleware [t-mw/token-auth-middleware t-mw/auth-middleware t-mw/admin-middleware]
      :parameters {:header t-schema/auth-header}
      :responses  {200 {:body t-schema/user-summaries}}
      :handler
      (fn [{{:keys [db]} :env}]
        {:status 200
         :body (t-core/get-users db)})}

     :put
     {:summary "Update the authenticated User's details"
      :description "Update the User based on the `:user/id` contained in the Token.
                   Requires an Authentication header <type> <credentials>.
                   Where the type is `Token` and the credentials are a signed `JWT`. e.g. Token YWxhZGRpbjpvcGVuc2VzYW1l..."
      :middleware [t-mw/token-auth-middleware t-mw/auth-middleware]
      :parameters {:body t-schema/user-details
                   :header t-schema/auth-header}
      :responses  {200 {:body t-schema/user-summary}}
      :handler (fn [req]
                 (let [db (-> req :env :db)
                       identity (-> req :identity ->identity-keys)
                       user-data (-> req :parameters :body)
                       user (->> identity
                                 (merge user-data)
                                 (t-core/update-user-details db))]
                   {:status 200
                    :body user}))}}]

   ["/:email"
    {:get
     {:summary "Get the authenticated User's details, or if you are an Admin, any User's details."
      :description "Get the User based on the `:user/id` contained in the Token.
                   Requires an Authentication header <type> <credentials>.
                   Where the type is `Token` and the credentials are a signed `JWT`.
                   e.g. Token YWxhZGRpbjpvcGVuc2VzYW1l..."
      :middleware [t-mw/token-auth-middleware t-mw/auth-middleware t-mw/permissions-middleware]
      :parameters {:path   t-schema/user-email
                   :header t-schema/auth-header}
      :responses  {200 {:body t-schema/user-summary}}
      :handler  (fn [req]
                  (let [db (-> req :env :db)
                        resource-email (->> req :parameters :path :email (assoc {} :user/email))
                        user (t-core/get-user-by-email db resource-email)]
                    {:status 200
                     :body user}))}

     :patch
     {:summary "Partially update a User's details - Requires Admin role."
      :description "Update a User's status to be `active` or `inactive`.
                   Requires an Authentication header <type> <credentials>.
                   Where the type is `Token` and the credentials are a signed `JWT`.
                   e.g. Token YWxhZGRpbjpvcGVuc2VzYW1l..."
      :middleware [t-mw/token-auth-middleware t-mw/auth-middleware t-mw/admin-middleware]
      :parameters {:path t-schema/user-email
                   :body t-schema/user-status-patch
                   :header t-schema/auth-header}
      :responses  {200 {:body t-schema/user-summary}}
      :handler (fn [req]
                 (let [db (-> req :env :db)
                       resource-email (->> req :parameters :path :email (assoc {} :user/email))
                       user-details (-> req :parameters :body)
                       user-id (-> (t-core/get-user-by-email db resource-email)
                                   (select-keys [:user/id]))
                       user  (->> user-id
                                  (merge user-details)
                                  (t-core/update-user-details db))]
                   {:status 200
                    :body user}))}

     :delete
     {:summary "Soft delete the authenticated User, or if you are an Admin, any User."
      :description "This is a soft delete where the User is updated to be `inactive`.
                    Get the User based on the `:user/id` contained in the Token.
                    Requires an Authentication header <type> <credentials>.
                    Where the type is `Token` and the credentials are a signed `JWT`. e.g. Token YWxhZGRpbjpvcGVuc2VzYW1l..."
      :middleware [t-mw/token-auth-middleware t-mw/auth-middleware t-mw/permissions-middleware]
      :parameters {:path   t-schema/user-email
                   :header t-schema/auth-header}
      :responses  {200 {:body t-schema/message}}
      :handler  (fn [req]
                  (let [db (-> req :env :db)
                        resource-email (->> req :parameters :path :email (assoc {} :user/email))
                        _ (t-core/delete-user-by-email db resource-email)]
                    {:status 200
                     :body   {:message (format "User %s has been deleted." (:user/email resource-email))}}))}}]])

(def user-action-routes
  ["/actions"
   {:swagger {:tags ["Users"]}}
   ["/update-user-status"
    {:put
     {:summary "Update a User's status to be `active` or `inactive` - Requires Admin role."
      :description "Requires an Authentication header <type> <credentials>.
                   Where the type is `Token` and the credentials are a signed `JWT`.
                   e.g. Token YWxhZGRpbjpvcGVuc2VzYW1l..."
      :middleware [t-mw/token-auth-middleware t-mw/auth-middleware t-mw/admin-middleware]
      :parameters {:body t-schema/user-status
                   :header t-schema/auth-header}
      :responses  {200 {:body t-schema/user-summary}}
      :handler (fn [req]
                 (let [db (-> req :env :db)
                       email (-> req :parameters :body (select-keys [:user/email]))
                       status (-> req :parameters :body (select-keys [:user/active]))
                       user-id (-> (t-core/get-user-by-email db email)
                                   (select-keys [:user/id]))
                       user (->> (merge status user-id)
                                 (t-core/update-user-details db))]
                   {:status 200
                    :body user}))}}]

   ["/update-user-roles"
    {:put
     {:summary "Update a User's Roles - Requires Admin role."
      :description "The User must be an Admin.
                   Requires an Authentication header <type> <credentials>.
                   Where the type is `Token` and the credentials are a signed `JWT`.
                   e.g. Token YWxhZGRpbjpvcGVuc2VzYW1l"
      :middleware [t-mw/token-auth-middleware t-mw/auth-middleware t-mw/admin-middleware]
      :parameters {:body t-schema/user-roles
                   :header t-schema/auth-header}
      :responses  {200 {:body t-schema/user-summary}}
      :handler (fn [req]
                 (let [db (-> req :env :db)
                       email (-> req :parameters :body (select-keys [:user/email]))
                       roles (-> req :parameters :body (select-keys [:user/roles]))
                       user-id (-> (t-core/get-user-by-email db email)
                                   (select-keys [:user/id]))
                       user (->> (merge roles user-id)
                                 (t-core/update-user-details db))]
                   {:status 200
                    :body   user}))}}]])

(def registration-routes
  ["/actions"
   {:swagger {:tags ["Users"]}}

   ["/register"
    {:post
     {:summary "Registers a User."
      :description "For an new User, creates a User with a `user` role and marks the Account status as inactive.
                   An email is sent containing instructions on how to complete the registration process.
                   The email includes a Time based OTP (one time password) that will expire after a configurable period of time.
                   For an inactive existing User, the same workflow is followed."
      :parameters {:body t-schema/registration}
      :responses  {201 {:body t-schema/message}}
      :handler  (fn [req]
                  (let [db (-> req :env :db)
                        notifier (-> req :env :notifier)
                        auth (-> req :env :auth)
                        user (-> req :parameters :body)
                        _ (t-core/register-user db notifier auth user)
                        email (:user/email user)]
                    {:status 201
                     :body   {:message (format "An email has been sent to %s with the details for completing your Registration." email)}}))}}]

   ["/confirm-registration"
    {:put
     {:summary "Confirms a Users Registration."
      :description "Changes the Users status to active. Requires a valid Time based OTP (one time password)
                   that was included in the Registration email."
      :middleware [t-mw/otp-middleware t-mw/auth-middleware]
      :parameters {:body t-schema/registration-confirmation}
      :responses  {200 {:body t-schema/message}}
      :handler  (fn [req]
                  (let [db (-> req :env :db)
                        notifier (-> req :env :notifier)
                        identity (-> req :identity)
                        _ (t-core/confirm-registration db notifier identity)]
                    {:status 200
                     :body   {:message "Thanks for confirming your registation! You can now login."}}))}}]])

(def login-routes
  ["/actions"
   {:swagger {:tags ["Users"]}}
   ["/login"
    {:post
     {:summary    "Log-in to the application."
      :description "Requires an Authentication header <type> <credentials>.
                   Where the type is `Basic` and the credentials are a Base64 encoded string with the format 'email:password'.
                   e.g. Basic bmVpbEB0ZW5mcmVuLmNvbToxMjM0NTY3OA==
                   The above creds are valid so try them.
                   The returned identity contains a signed `JWT` token used to authenticate the user in subsequent calls."
      :middleware [t-mw/basic-auth-middleware t-mw/auth-middleware]
      :parameters {:header t-schema/auth-header}
      :responses  {200 {:body t-schema/user-summary-token}}
      :handler
      (fn [req]
        (let [db (-> req :env :db)
              identity (-> req :identity)]
          (t-core/successful-login db identity)
          {:status 200 :body identity}))}}]])

(def password-routes
  ["/actions"
   {:swagger {:tags ["Users"]}}
   ["/request-password-reset"
    {:post
     {:summary    "Request a Password reset."
      :description "Sends an  email containing instructions on how to change the password.
                   The email includes a Time based OTP (one time password) that will expire after a configurable period of time."
      :parameters {:body t-schema/password-reset-request}
      :responses  {200 {:body t-schema/message}}
      :handler    (fn [req]
                    (let [db (-> req :env :db)
                          notifier (-> req :env :notifier)
                          auth (-> req :env :auth)
                          email (-> req :parameters :body :email)
                          _ (t-core/request-password-reset db notifier auth {:user/email email})]
                      {:status 200
                       :body   {:message (format "An email has been sent to %s with the details for changing your password." email)}}))}}]

   ["/reset-password"
    {:put
     {:summary    "Resets a User's password."
      :description "Changes the Users password. Requires a valid Time based OTP (one time password)
                   that was included in the Password reset email."
      :middleware [t-mw/otp-middleware t-mw/auth-middleware]
      :parameters {:body t-schema/password-reset}
      :responses  {200 {:body t-schema/message}}
      :handler    (fn [req]
                    (let [db (-> req :env :db)
                          identity (-> req :identity ->identity-keys)
                          password (-> req :parameters :body :password)
                          email (:user/email identity)
                          _    (t-core/reset-password db identity password)]
                      {:status 200
                       :body   {:message (format "The password for %s has been changed." email)}}))}}]

   ["/change-password"
    {:put
     {:summary    "Changes a User's password."
      :description "Requires an Authentication header <type> <credentials>.
                   Where the type is `Token` and the credentials are a signed `JWT`. e.g. Token YWxhZGRpbjpvcGVuc2VzYW1l.."
      :middleware [t-mw/token-auth-middleware t-mw/auth-middleware]
      :parameters {:body t-schema/password-change
                   :header t-schema/auth-header}
      :responses  {200 {:body t-schema/message}}
      :handler    (fn [req]
                    (let [db (-> req :env :db)
                          identity (-> req :identity ->identity-keys)
                          old-password (-> req :parameters :body :old-password)
                          new-password (-> req :parameters :body :new-password)
                          email (:user/email identity)
                          _  (t-core/change-password db identity old-password new-password)]
                      {:status 200
                       :body   {:message (format "The password for %s has been changed." email)}}))}}]])

;; application setup

(defn app
  "Configures routes, common middleware.
  Specific middleware i.e.`t-middleware/token-auth-middleware` is added on a route by route basis.
  The supplied `env` is added to the `http request` via `t-middleware/env-middleware`
  and then route handlers will pull out what they need. i.e. DB connection, `notifier`, auth configuration.
  Alternatively the `env` could be passed into each route handler and middleware as an arg."
  [env]
  (ring/ring-handler
   (ring/router
    [swagger-routes
     ["/api/v0"
      user-routes
      user-action-routes
      login-routes
      registration-routes
      password-routes
      system-routes]]
    {;;:reitit.middleware/transform dev/print-request-diffs
     ;; :conflicts (fn [conflicts]
     ;;              (println (exception/format-exception :path-conflicts nil conflicts)))
     :exception pretty/exception
     :data {:coercion (reitit.coercion.malli/create
                       {:compile mu/closed-schema
                        :strip-extra-keys true
                        :default-values false
                          ;; malli options
                        :options nil})
            :muuntaja m/instance
            :env (-> env (update :db t-db/default-ds-options))
            :middleware [swagger/swagger-feature
                         t-mw/env-middleware
                         parameters/parameters-middleware
                         muuntaja/format-negotiate-middleware
                         muuntaja/format-response-middleware
                         t-mw/exception-middleware
                         muuntaja/format-request-middleware
                         muuntaja.middleware/wrap-params
                         coercion/coerce-response-middleware
                         coercion/coerce-request-middleware
                         t-mw/cors-middleware
                         #(logger/wrap-with-logger % {:redact-key? #{:password :user/password
                                                                     :token :user/token
                                                                     :token-key :user/token-key}})]}})

   (ring/routes
    (swagger-ui/create-swagger-ui-handler
     {:path "/"
      :config {:validatorUrl nil
               :operationsSorter "alpha"}})
    (ring/create-default-handler
     {:not-found (constantly {:status 404 :body "Not found"})}))))

;; TODO would these helpers reduce readability of the routes?

;; (defn ->identity [req] (-> req :identity))

;; (defn ->db [req]
;;   (-> req :env :db))

;; (defn ->notifier [req]
;;   (-> req :env :notifier))

;; (defn ->auth [req]
;;   (-> req :env :auth))

;; (defn ->body [req]
;;   (-> req :parameters :body))

;; (defn ->path [req]
;;   (-> req :parameters :path))
