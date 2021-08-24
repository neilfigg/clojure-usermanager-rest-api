(ns tenfren.core
  "Business logic and controlling calls to `db`."
  (:require
   [buddy.hashers :as hashers]
   ;;[clojure.tools.logging :as log]
   [next.jdbc :as jdbc]
   [tenfren.db :as t-db]
   [tenfren.notifier :as t-notifier]
   [tenfren.security :as t-security]
   [tick.alpha.api :as tick]))

;; User

(def default-roles ["user"])

(defn- check-not-registered [user]
  (when (:user/active user)
    (throw (ex-info "User is already registered." {:type :registration-conflict})))
  user)

(defn- update-user
  "Update a User's record. Returns the updated User."
  [db user]
  (let [user-id (select-keys user [:user/id])]
    (if (and (:user/id user-id)
             (not (zero? (:user/id user-id))))
      (t-db/save-user db user)
      (throw (ex-info "Tried to update a User without an Id."
                      {:type :missing-user-id})))))

;;; api

(defn find-user-by-email
  "Find the user by email.
  If a User is not found a nil is returned.
  See also [[get-user-by-email]] for a call that throws an exception if the User is not found."
  [db email]
  (jdbc/with-transaction [tx db {:read-only true}]
    (let [tx-opts (t-db/default-ds-options tx)]
      (t-db/get-user-by-email tx-opts email))))

(defn get-users
  "Get all the Users."
  [db]
  (jdbc/with-transaction [tx db {:read-only true}]
    (let [tx-opts (t-db/default-ds-options tx)]
      (t-db/get-users tx-opts))))

(defn get-user-by-id
  "Find the user by id.
  If the User is not found an exception is thrown."
  [db id]
  (jdbc/with-transaction [tx db {:read-only true}]
    (let [tx-opts (t-db/default-ds-options tx)]
      (if-let [user (t-db/get-user-by-id tx-opts id)]
        user
        (throw (ex-info "User not found by id."
                        {:type :user-not-found}))))))

(defn get-user-by-email
  "Find the user by email.
  If the User is not found an exception is thrown.
  If the User doesn't own the resource, or they are not an Admin, throw an exception.
  See also [[find-user-by-email]] for a call that returns a nil if the User is not found."
  [db email]
  (jdbc/with-transaction [tx db {:read-only true}]
    (let [tx-opts (t-db/default-ds-options tx)]
      (if-let [user (t-db/get-user-by-email tx-opts email)]
        user
        (throw (ex-info "User not found by email."
                        {:type :user-not-found}))))))

(defn update-user-details
  "Update a User's details."
  [db user]
  (jdbc/with-transaction [tx db]
    (let [tx-opts (t-db/default-ds-options tx)]
      (update-user tx-opts user))))

(defn delete-user-by-email
  "Delete the User by email.
  If the User is not found an exception is thrown."
  [db email]
  (jdbc/with-transaction [tx db]
    (let [tx-opts (t-db/default-ds-options tx)]
      (t-db/delete-user-by-email tx-opts email))))

(defn create-confirmation-details
  "Create the details need so a User can confirm an action.
  e.g.
  ```clojure
  {:token 123456
   :expires-in-secs 60}
  ```"
  [auth user]
  (t-security/create-totp-token-details
   (:user/token-key user)
   (:totp-step-secs auth)))

(defn register-user
  "Registers a User for access to the application.
  For a new User, or a User who hasn't completed their Registration:
   - insert/update the User in the DB, with an `active` status of false
   - create a TOTP key
   - insert default permissions
   - send an email with a details to complete the Registration.
   - Returns the User."
  [db notifier auth user]
  (jdbc/with-transaction [tx db]
    (let [tx-opts (t-db/default-ds-options tx)
          token-key (t-security/generate-otp-secret-key)
          user (-> (if-let [existing-user (find-user-by-email tx-opts user)]
                     (merge existing-user user)
                     user)
                   (check-not-registered)
                   (update :user/password hashers/encrypt)
                   (assoc :user/token-key token-key
                          :user/roles default-roles))
          user (t-db/save-user tx-opts user)
          params (create-confirmation-details auth user)]
      (t-notifier/registration-notification notifier user params)
      user)))

(defn confirm-registration
  "Confirms a Users registration.
    - update the User in the DB, with an `active` status of true:
    - send a welcome email."
  [db notifier user]
  (jdbc/with-transaction [tx db]
    (let [tx-opts (t-db/default-ds-options tx)
          user (->> user
                    (get-user-by-email tx-opts)
                    (check-not-registered))]
      (update-user tx-opts (-> user
                               (assoc :user/active true)
                               (select-keys [:user/id
                                             :user/active])))
      (t-notifier/registration-confirmation-notification notifier user))))

(defn request-password-reset
  "Process a request to reset a password.
  An email will be sent to the User that contains a time based one-time-password (TOTP)
  they will need to reset the password."
  [db notifier auth email]
  (jdbc/with-transaction [tx db {:read-only true}]
    (let [tx-opts (t-db/default-ds-options tx)
          user (get-user-by-email tx-opts email)
          params (create-confirmation-details auth user)]
      (t-notifier/password-reset-request-notification notifier user params))))

(defn reset-password
  "Update a User's password."
  [db user password]
  (jdbc/with-transaction [tx db]
    (let [tx-opts (t-db/default-ds-options tx)
          user (-> user
                   (assoc :user/password (hashers/encrypt password))
                   (select-keys [:user/id
                                 :user/password]))]
      (update-user tx-opts user))))

(defn change-password
  "Update a User's password if the supplied old password is correct."
  [db user old-password new-password]
  (jdbc/with-transaction [tx db]
    (let [tx-opts (t-db/default-ds-options tx)
          user (get-user-by-id tx-opts user)]
      (if (hashers/check old-password (:user/password user))
        (let [user (-> user
                       (assoc :user/password (hashers/encrypt new-password))
                       (select-keys [:user/id
                                     :user/password]))]
          (update-user tx-opts user))
        (throw (ex-info "Current password does not match."
                        {:type :password-mismatch}))))))

(defn successful-login
  "Update User details for a successful login.
   Returns the updated User."
  [db user]
  (jdbc/with-transaction [tx db]
    (let [tx-opts (t-db/default-ds-options tx)
          user (-> user
                   (select-keys [:user/id])
                   (assoc :user/failed-login-attempts 0
                          :user/lockout-time nil
                          :user/last-login-timestamp (tick/date-time)))]
      (update-user tx-opts user))))

(defn unsuccessful-login
  "Update User details about an unsuccessful login.
   Returns the updated User."
  [db user max-attempts authentication-lockout-ms]
  (jdbc/with-transaction [tx db]
    (let [tx-opts (t-db/default-ds-options tx)
          failed-attempts (inc (:user/failed-login-attempts user))
          expires (tick/>> (tick/now) (tick/new-duration authentication-lockout-ms :millis))
          lockout-time (when (> failed-attempts max-attempts) expires)
          user (-> user
                   (select-keys [:user/id])
                   (assoc :user/failed-login-attempts failed-attempts
                          :user/lockout-time lockout-time))]
      (update-user tx-opts user))))
