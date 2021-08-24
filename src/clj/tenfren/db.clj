(ns tenfren.db
  "Controlling access to the underlying datastore."
  (:require
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]
   [next.jdbc.sql :as sql]
   [tick.alpha.api :as tick]))

;; db options

(defn- map-table-ns
  "Map the table ns from `account` to `user`.
  I had rewritten the API and forgot that `user` is a reserved name in Postgres.
  So changing the namespace that `next.jdbc result_set` derives from the `Account` table."
  [^String s]
  (if (= s "account") "user" s))

(def tenfren-opts
  "DB options."
  (assoc jdbc/snake-kebab-opts
         :builder-fn rs/as-modified-maps
         :qualifier-fn map-table-ns))

(defn default-ds-options
  "Convert between kebab case (clojure) and snake case (db). "
  [ds]
  (jdbc/with-options ds tenfren-opts))

;; Util

(defn reduce-user
  "1:m relationship between a `User` and `Role` tables. So if a User has 2 roles the SQL Result set is a vector of maps like ...
  `
  [{:user/id 1
    :user/email neil@tenfren.com
    :role/name admin}
   {:user/id 1
    :user/email neil@tenfren.com
    :role/name user}]
 `
  We want to reduce the result to a single map with the roles in a vector like ...

  `[{:user/id 1,
     :user/email neil@tenfren.com
     :roles [admin user]}]`

  TODO requiring 2 passes of the data 1) reduce 2) mapv. Can we do in one pass?"
  [users]
  (->> users
       (reduce
        (fn [acc m]
          (let [{:keys [user/id]} m]
            (if (contains? acc id)
              (update-in acc [id :user/roles] (fnil conj []) (:role/name m))
              (let [role (:role/name m)
                    m (-> m
                          (assoc :user/roles [role])
                          (dissoc :role/name))]
                (assoc acc id m)))))
        {})
       (mapv (fn [m] (-> m val)))))

;; Roles

(defn- delete-roles-by-user-id
  "Hard delete"
  [tx user-id]
  (jdbc/execute! tx ["DELETE FROM role WHERE account_id = ?" user-id]))

(defn- update-user-roles
  "Taking the blunt approach here and deleting existing Roles before adding new ones.
  So the roles passed in need to be all the roles for that user."
  [ds user-id roles]
  (let [now (tick/date-time)
        id (:user/id user-id)
        roles (->> roles
                   :user/roles
                   (mapv (fn [role] [id role true now now])))]
    (delete-roles-by-user-id ds id)
    (sql/insert-multi! ds :role [:account-id :name :active :created-timestamp :modified-timestamp] roles)))

;; Users


(defn get-user-by-email
  "Get the User and Roles by the email. The `account` namespace is changed to `user`.
  Using `ILIKE` for a case-insensitive search."
  [ds email]
  (some->>
   (jdbc/plan ds ["select account.*, role.name
                  from account
                  left join role on account.id = role.account_id
                  where account.email ILIKE ?" (:user/email email)])
   reduce-user
   first))

(defn get-user-by-id
  "Get the User and Roles by the User Id. The `account` ns is changed to `user`."
  [ds id]
  (some->>
   (jdbc/plan ds ["select account.*, role.name
                  from account
                  left join role on account.id = role.account_id
                  where account.id = ?" (:user/id id)])
   reduce-user
   first))

(defn get-users
  "Get all the Users and their associated Roles. The `account` ns is changed to `user`."
  [ds]
  (some->>
   (jdbc/plan ds ["select account.*, role.name
                  from account
                  left join role on account.id = role.account_id"])
   reduce-user))

(defn- insert-user
  "Inserts a User and returns the user-id `#:user{:id 4}`.
  Actually H2 returns the new user-id, while PostgreSQL returns the new record."
  [ds user]
  (let [now  (tick/date-time)
        user (-> user
                 (dissoc :user/id
                         :user/roles
                         :user/last-login-timestamp)
                 (assoc :user/created-timestamp now
                        :user/modified-timestamp now))]
    (sql/insert! ds :account user)))

(defn- add-user
  "Insert a new User and the associated Roles."
  [ds user]
  (let [user-id (insert-user ds user)
        user-roles (select-keys user [:user/roles])]
    (update-user-roles ds user-id user-roles)
    (get-user-by-id ds user-id)))

(defn- update-user
  "Update the User details and associated Roles."
  [ds user]
  (let [now  (tick/date-time)
        user-id (select-keys user [:user/id])
        user-roles (select-keys user [:user/roles])
        user (-> user
                 (dissoc :user/id
                         :user/roles
                         :user/created-timestamp)
                 (assoc :user/modified-timestamp now))]
    (sql/update! ds :account user {:id (:user/id user-id)})
    (when (seq (:user/roles user-roles))
      (update-user-roles ds user-id user-roles))
    (get-user-by-id ds user-id)))

(defn save-user
  "Save a user record.
  If the ID is present, and not zero, then this is an update operation, otherwise it's an insert.
  Returns the saved User.
  Call within a transaction."
  [ds user]
  (let [id (:user/id user)]
    (if (and id (not (zero? id)))
      (update-user ds user)
      (add-user ds user))))

(defn delete-user-by-id
  "This is a soft delete where the account and roles are inactivated.
  Call within a transaction."
  [ds user-id]
  (jdbc/execute-one! ds ["UPDATE role set active = false WHERE account_id = ? and active = true" (:user/id user-id)])
  (jdbc/execute-one! ds ["UPDATE account set active = false WHERE id = ? and active = true" (:user/id user-id)]))

(defn delete-user-by-email
  "This is a soft delete where the account and roles are inactivated.
  Call within a transaction."
  [ds user-email]
  (let [user (get-user-by-email ds user-email)
        id (select-keys user [:user/id])]
    (delete-user-by-id ds id)))

(defn create-db
  "Creates the DB."
  [ds]
  (log/info "DB creation start")
  (let [schema (slurp (io/resource "db-schema/tenfren.ddl"))]
    (jdbc/execute! ds [schema]))
  (log/info "DB creation done"))
