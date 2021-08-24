(ns tenfren.handler-test
  "Tests for `handler.clj`"
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [clojure.tools.logging :as log]
   [malli.core :as m]
   [muuntaja.core :as mc]
   [ring.mock.request :as mock]
   [ring.util.codec :as codec]
   [tenfren.schema :as t-schema]
   [tenfren.test-fixtures :refer [create-jwt-token create-totp-token gen-user handler init-db max-authentication-attempts]]
   [tick.alpha.api :as tick]))

(use-fixtures :each init-db)

;;; helpers

(defn <-json [m] (mc/decode "application/json" m))

(defn ->base64 [s] (codec/base64-encode (.getBytes s)))

(defn ->url-encoded [s] (codec/url-encode s)) ;; default is UTF-8

(defn auth-header
  "Adds Authorization header to the request
  with base64 encoded \"Basic user:pass\" value."
  [req user password]
  (let [token (->base64 (format "%s:%s" user password))]
    (mock/header req "Authorization" (str "Basic " token))))

(defn token-header
  [req token]
  (mock/header req "Authorization" (str "Token " token)))

(defn json-content-type?
  "Returns true if the content type of the ring response is JSON"
  [resp]
  (= "application/json; charset=utf-8" (get-in resp [:headers "Content-Type"])))

(defn expected-message?
  [resp msg]
  (= msg (:message (<-json (:body resp)))))

(defn expected-error?
  [resp msg]
  (= msg (:error (<-json (:body resp)))))

;;; tests

(deftest  ^:info info
  (let [get-system-info (fn []
                          (handler (-> (mock/request :get "/api/v0/system/info")
                                       (mock/content-type "application/json"))))]
    (testing  "system info"
      (let [resp (get-system-info)]
        (is (= 200 (:status resp)))
        (is (json-content-type? resp))))))

(deftest  ^:registration registration
  (let [register-user-api (fn [user]
                            (handler (-> (mock/request :post "/api/v0/actions/register")
                                         (mock/content-type "application/json")
                                         (mock/json-body user))))]
    (testing  "new user"
      (let [user (gen-user {:schema t-schema/new-user})
            resp (register-user-api user)
            email (:user/email user)
            expected-msg (format "An email has been sent to %s with the details for completing your Registration." email)]
        (is (= 201 (:status resp)))
        (is (json-content-type? resp))
        (is (expected-message? resp expected-msg))))

    (testing  "unconfirmed existing user"
      (let [user (gen-user {:schema t-schema/new-user :db? true})
            resp (register-user-api user)
            email (:user/email user)
            expected-msg (format "An email has been sent to %s with the details for completing your Registration." email)]
        (is (= 201 (:status resp)))
        (is (json-content-type? resp))
        (is (expected-message? resp expected-msg))))

    (testing "confirmed existing user"
      (let [user (-> (gen-user {:schema t-schema/new-user :db? true :active? true}))
            resp (register-user-api user)]
        (is (= 409 (:status resp)))
        (is (json-content-type? resp))
        (is (expected-message? resp "User is already registered."))))

    (testing "missing request data"
      (let [user (-> (gen-user {:schema t-schema/new-user})
                     (dissoc :user/email))
            resp (register-user-api user)]
        (is (= 400 (:status resp)))
        (is (json-content-type? resp))))))

(deftest ^:registration-confirmation registration-confirmation
  (let [confirm-registration-api (fn [email token]
                                   (let [params  {:email email
                                                  :verification-code token}]
                                     (handler (-> (mock/request :put "/api/v0/actions/confirm-registration")
                                                  (mock/content-type "application/json")
                                                  (mock/json-body params)))))]

    (testing "unconfirmed user"
      (let [user (gen-user {:schema t-schema/new-user :db? true})
            token (create-totp-token (:user/token-key user))
            email (:user/email user)
            resp (confirm-registration-api email token)]
        (is (= 200 (:status resp)))
        (is (json-content-type? resp))
        (is (expected-message? resp "Thanks for confirming your registation! You can now login."))))

    (testing "confirmed user"
      (let [user (gen-user {:schema t-schema/new-user :db? true :active? true})
            token (create-totp-token (:user/token-key user))
            email (:user/email user)
            resp (confirm-registration-api email token)]
        (is (= 409 (:status resp)))
        (is (json-content-type? resp))
        (is (expected-message? resp "User is already registered."))))

    (testing "invalid TOTP token"
      (let [user (gen-user {:schema t-schema/new-user :db? true :active? true})
            token 123
            email (:user/email user)
            resp (confirm-registration-api email token)]
        (is (= 401 (:status resp)))
        (is (json-content-type? resp))))

    (testing "unknown user"
      (let [user {:user/email "some@user.com" :user/token-key "A4I774XAQM36J7IL"}
            token (create-totp-token (:user/token-key user))
            email (:user/email user)
            resp (confirm-registration-api email token)]
        (is (= 401 (:status resp)))
        (is (json-content-type? resp))
        (is (expected-error? resp "Invalid Verification Code"))))))

(deftest ^:login login
  (let [login-api (fn [username password]
                    (handler (-> (mock/request :post "/api/v0/actions/login")
                                 (mock/content-type "application/json")
                                 (auth-header username password))))]
    (testing "confirmed user"
      (let [user (gen-user {:schema t-schema/new-user :roles ["user"] :db? true :active? true})
            username (:user/email user)
            password (:user/password user)
            resp (login-api username password)
            body (<-json (:body resp))]
        (is (= 200 (:status resp)))
        (is (json-content-type? resp))
        (is (m/validate t-schema/user-summary-token body))))

    (testing "confirmed user after lockout time period has expired"
      (let [lockout-time-in-past (tick/<< (tick/now) (tick/new-duration 1 :days))
            user (gen-user {:schema t-schema/new-user :roles ["user"] :failed-login-attempts 4 :lockout-time lockout-time-in-past :db? true :active? true})
            username (:user/email user)
            password (:user/password user)
            resp (login-api username password)
            body (<-json (:body resp))]
        (is (= 200 (:status resp)))
        (is (json-content-type? resp))
        (is (m/validate t-schema/user-summary-token body))))

    (testing  "confirmed user after prior login failures"
      (let [user (gen-user {:schema t-schema/new-user :roles ["user"] :failed-login-attempts (dec max-authentication-attempts) :db? true :active? true})
            username (:user/email user)
            password "invalid password"
            resp (login-api username password)
            body (<-json (:body resp))]
        (is (= 401 (:status resp)))
        (is (json-content-type? resp))))

    (testing "confirmed user locked out"
      (let [user (gen-user {:schema t-schema/new-user :roles ["user"] :failed-login-attempts max-authentication-attempts :db? true :active? true})
            username (:user/email user)
            password "invalid password"
            resp (login-api username password)
            resp (login-api username password)
            body (<-json (:body resp))]
        (is (= 403 (:status resp)))
        (is (json-content-type? resp))))

    (testing "unconfirmed user"
      (let [user (gen-user {:schema t-schema/new-user :db? true})
            username (:user/email user)
            password (:user/password user)
            resp (login-api username password)]
        (is (= 401 (:status resp)))
        (is (json-content-type? resp))))

    (testing "non existent User"
      (let [username "not a user"
            password "not a password"
            resp (login-api username password)]
        (is (= 401 (:status resp)))))

    (testing "invalid password"
      (let [user (gen-user {:schema t-schema/new-user :roles ["user"] :db? true :active? true})
            username (:user/email user)
            password "invalid password"
            resp (login-api username password)
            body (<-json (:body resp))]
        (is (= 401 (:status resp)))
        (is (json-content-type? resp))))))

(deftest  ^:password password
  (let [request-password-reset-api (fn [email]
                                     (let [params {:email email}]
                                       (handler (-> (mock/request :post "/api/v0/actions/request-password-reset")
                                                    (mock/content-type "application/json")
                                                    (mock/json-body params)))))
        reset-password-api (fn [email password token]
                             (let [params {:email email
                                           :password password
                                           :verification-code token}]
                               (handler (-> (mock/request :put "/api/v0/actions/reset-password")
                                            (mock/content-type "application/json")
                                            (mock/json-body params)))))
        change-password-api (fn [old-pwd new-pwd token]
                              (let [params {:old-password old-pwd
                                            :new-password new-pwd}
                                    _ (log/info "@@@@@@@  params" params)]
                                (handler (-> (mock/request :put "/api/v0/actions/change-password")
                                             (mock/content-type "application/json")
                                             (mock/json-body params)
                                             (token-header token)))))]
    ;; reset

    (testing "password reset request"
      (let [user (gen-user {:schema t-schema/new-user :active? true :db? true})
            email (:user/email user)
            resp (request-password-reset-api email)
            expected-msg (format "An email has been sent to %s with the details for changing your password." email)]
        (is (= 200 (:status resp)))
        (is (json-content-type? resp))
        (is (expected-message? resp expected-msg))))

    (testing  "password reset"
      (let [user (gen-user {:schema t-schema/new-user :db? true :active? true})
            email (:user/email user)
            password (:user/password user)
            token (create-totp-token (:user/token-key user))
            resp (reset-password-api email password token)
            expected-msg (format "The password for %s has been changed." email)]
        (is (= 200 (:status resp)))
        (is (json-content-type? resp))
        (is (expected-message? resp expected-msg))))

    (testing  "password reset invalid token"
      (let [user (gen-user {:schema t-schema/new-user :db? true :active? true})
            email (:user/email user)
            password (:user/password user)
            token 123
            resp (reset-password-api email password token)]
        (is (= 401 (:status resp)))
        (is (json-content-type? resp))))

    (testing  "password reset invalid email"
      (let [user (gen-user {:schema t-schema/new-user :db? true :active? true})
            email "invlaid-email"
            password (:user/password user)
            token (create-totp-token (:user/token-key user))
            resp (reset-password-api email password token)]
        (is (= 400 (:status resp)))
        (is (json-content-type? resp))))

    (testing  "password reset invalid short password"
      (let [user (gen-user {:schema t-schema/new-user :db? true :active? true})
            email (:user/email user)
            password "7chars!"
            token (create-totp-token (:user/token-key user))
            resp (reset-password-api email password token)]
        (is (= 400 (:status resp)))
        (is (json-content-type? resp))))

    (testing  "password reset invalid long password"
      (let [user (gen-user {:schema t-schema/new-user :db? true :active? true})
            email (:user/email user)
            password "17chars!!!!!!!!!!"
            token (create-totp-token (:user/token-key user))
            resp (reset-password-api email password token)]
        (is (= 400 (:status resp)))
        (is (json-content-type? resp))))

    ;; change

    (testing  "password change"
      (let [user (gen-user {:schema t-schema/new-user :db? true :active? true})
            email (:user/email user)
            old-pwd (:user/password user)
            new-pwd "12345678"
            token (create-jwt-token user)
            resp (change-password-api old-pwd new-pwd token)
            expected-msg (format "The password for %s has been changed." email)]
        (is (= 200 (:status resp)))
        (is (json-content-type? resp))
        (is (expected-message? resp expected-msg))))

    (testing  "password change invalid password"
      (let [user (gen-user {:schema t-schema/new-user :db? true :active? true})
            old-pwd "invalid-pwd"
            new-pwd "12345678"
            token (create-jwt-token user)
            resp (change-password-api old-pwd new-pwd token)
            expected-msg  "Current password does not match."]
        (is (= 403 (:status resp)))
        (is (json-content-type? resp))
        (is (expected-message? resp expected-msg))))))

(deftest ^:users users
  (let [update-user-status-api (fn [token user-data]
                                 (handler (-> (mock/request :put (str "/api/v0/actions/update-user-status"))
                                              (mock/content-type "application/json")
                                              (mock/json-body user-data)
                                              (token-header token))))

        update-user-roles-api (fn [token user-data]
                                (handler (-> (mock/request :put (str "/api/v0/actions/update-user-roles"))
                                             (mock/content-type "application/json")
                                             (mock/json-body user-data)
                                             (token-header token))))

        patch-update-user-api (fn [token user-data email]
                                (let [encoded-email (-> email ->url-encoded)]
                                  (handler (-> (mock/request :patch (str "/api/v0/users/" encoded-email))
                                               (mock/content-type "application/json")
                                               (mock/json-body user-data)
                                               (token-header token)))))

        update-user-api (fn [token user-data]
                          (handler (-> (mock/request :put (str "/api/v0/users"))
                                       (mock/content-type "application/json")
                                       (mock/json-body user-data)
                                       (token-header token))))

        get-user-api (fn [token email]
                       (let [encoded-email (-> email ->url-encoded)]
                         (handler (-> (mock/request :get (str "/api/v0/users/" encoded-email))
                                      (mock/content-type "application/json")
                                      (token-header token)))))
        get-all-users-api (fn [token]
                            (handler (-> (mock/request :get "/api/v0/users")
                                         (mock/content-type "application/json")
                                         (token-header token))))

        delete-user-api (fn [token email]
                          (let [encoded-email (-> email ->url-encoded)]
                            (handler (-> (mock/request :delete (str "/api/v0/users/" encoded-email))
                                         (mock/content-type "application/json")
                                         (token-header token)))))]

    (testing "current user can update"
      (let [user (gen-user {:schema t-schema/new-user :roles ["user"] :active? true :db? true})
            token (create-jwt-token user)
            user-data {:user/first-name "neil"
                       :user/last-name "figg"
                       :user/screen-name "figgy"}
            resp (update-user-api token user-data)
            user-data2 (<-json (:body resp))]
        (is (= 200 (:status resp)))))

    (testing "current user can get"
      (let [user (gen-user {:schema t-schema/new-user :active? true :db? true})
            token (create-jwt-token user)
            email (:user/email user)
            resp (get-user-api token email)
            user2 (<-json (:body resp))]
        (is (= 200 (:status resp)))))

    (testing "current user can delete"
      (let [user (gen-user {:schema t-schema/new-user :active? true :db? true})
            token (create-jwt-token user)
            email (:user/email user)
            resp (delete-user-api token email)
            user2 (<-json (:body resp))]
        (is (= 200 (:status resp)))))

    (testing "current user can't update with invalid token"
      (let [token "eyJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJ0ZW..."
            user-data {:user/first-name "neil"
                       :user/last-name "figg"
                       :user/screen-name "figgy"}
            resp (update-user-api token user-data)]
        (is (= 401 (:status resp)))))

    (testing "current user can't get with invalid token"
      (let [user (gen-user {:schema t-schema/new-user :active? true :db? true})
            email (:user/email user)
            token "eyJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJ0ZW..."
            resp (get-user-api token email)]
        (is (= 401 (:status resp)))))

    (testing "admin user can update another user's status using action"
      (let [admin (gen-user {:schema t-schema/user :roles ["admin" "user"] :active? true :db? true})
            token (create-jwt-token admin)
            user (gen-user {:schema t-schema/user :roles ["user"] :active? true :db? true})
            user-status {:user/active false}
            user* (-> user
                      (select-keys [:user/email])
                      (merge user-status))
            resp (update-user-status-api token user*)
            user-data2 (<-json (:body resp))]
        (is (= 200 (:status resp)))))

    (testing "admin user can update another user's status using patch"
      (let [admin (gen-user {:schema t-schema/user :roles ["admin" "user"] :active? true :db? true})
            token (create-jwt-token admin)
            user (gen-user {:schema t-schema/user :roles ["user"] :active? true :db? true})
            email (:user/email user)
            user-status {:user/active false}
            user* (-> user
                      (select-keys [:user/email])
                      (merge user-status))
            resp (patch-update-user-api token user* email)
            user* (<-json (:body resp))]
        (is (= 200 (:status resp)))))

    (testing "admin user can updated another user's roles"
      (let [admin (gen-user {:schema t-schema/user :roles ["admin" "user"] :active? true :db? true})
            token (create-jwt-token admin)
            user (gen-user {:schema t-schema/user :roles ["user"] :active? true :db? true})
            user-roles {:user/roles ["user" "new-from-test"]}
            user* (-> user
                      (select-keys [:user/email])
                      (merge user-roles))
            resp (update-user-roles-api token user*)
            user-data2 (<-json (:body resp))]
        (is (= 200 (:status resp)))))

    (testing "admin user can get another user's details"
      (let [admin (gen-user {:schema t-schema/new-user :roles ["admin" "user"] :active? true :db? true})
            token (create-jwt-token admin)
            user (gen-user {:schema t-schema/new-user :roles ["user"] :active? true :db? true})
            email (:user/email user)
            resp (get-user-api token email)
            user2 (<-json (:body resp))]
        (is (= 200 (:status resp)))
        (is (= (:user/id user) (:user/id user2)))))

    (testing "admin user can get all users"
      (let [admin (gen-user {:schema t-schema/new-user :roles ["admin" "user"] :active? true :db? true})
            token (create-jwt-token admin)
            resp (get-all-users-api token)
            user (<-json (:body resp))]
        (is (= 200 (:status resp)))))

    (testing "non admin user can't get another user's details"
      (let [non-admin (gen-user {:schema t-schema/new-user :roles ["user"] :active? true :db? true})
            token (create-jwt-token non-admin)
            user (gen-user {:schema t-schema/new-user :roles ["user"] :active? true :db? true})
            email (:user/email user)
            resp (get-user-api token email)
            user2 (<-json (:body resp))]
        (is (= 403 (:status resp)))))

    (testing "non-admin user can't get all users"
      (let [user (gen-user {:schema t-schema/new-user :roles ["user"] :active? true :db? true})
            token (create-jwt-token user)
            resp (get-all-users-api token)]
        (is (= 403 (:status resp)))))))

(comment
  clj -M:test:test-kaocha --focus-meta :password
  clj -M:test:test-kaocha
  ./run-tests.sh)
