(ns scratch)

;; aggregate a vector of maps. In this case SQL result sets.

(comment
  (do
    (require '[clojure.pprint :as pp :refer [pprint] :rename {pprint p}])
    (defn reduce-user
      [users]
      (->> (reduce
            (fn [acc m]
              (let [{:keys [user/id]} m]
                (if (contains? acc id)
                  (update-in acc [id :user/roles] (fnil conj []) (:role/name m))
                  (let [role (:role/name m)
                        m (-> m
                              (assoc :user/roles [role])
                              (dissoc :role/name))]
                    (assoc acc id m)))))
            {}
            users)
           (mapv (fn [m] (-> m val)))))

    (def users
      [{:user/id 1,
        :user/email "neil1@tenfren",
        :role/name "1-a"}
       {:user/id 2,
        :user/email "neil2@tenfren",
        :role/name "2-a"}
       {:user/id 2,
        :user/email "neil2@tenfren",
        :role/name "2-b"}
       {:user/id 3,
        :user/email "neil3@tenfren",
        :role/name "3-a"}
       {:user/id 3,
        :user/email "neil3@tenfren",
        :role/name "3-b"}
       {:user/id 3,
        :user/email "neil3@tenfren",
        :role/name "3-c"}
       {:user/id 0,
        :user/email "neil0@tenfren",
        :role/name nil}])

    (p (reduce-user users)))

  (do
    (in-ns 'user)
    (require '[buddy.hashers :as hashers]
             '[next.jdbc :as jdbc]
             '[next.jdbc.sql :as sql]
             '[clojure.pprint :as pp :refer [pprint] :rename {pprint p}]
             '[integrant.repl.state :as state]
             '[tenfren.db :as t-db]
             '[tick.alpha.api :as tick]
             '[clojure.repl :refer [dir doc]])

  ;; replace jdbc/plan with jdbc/execute! to execute without a supplied reducer
  ;; clj -M:dev
  ;; user=> (go)

    (defn system [] (or state/system (throw (ex-info "System not running" {}))))
    (def ds (-> (:tenfren/db (system)) (t-db/default-ds-options))) ;; maps DB ns from account to user
    (def db ds)

    (defn get-user-by-id
      [db id]
      (some->>
       (jdbc/plan db ["select account.id, account.email, role.name
                  from account
                  left join role on account.id = role.account_id
                  where account.id = ?" id])
       reduce-user
       first))

    (p (get-user-by-id db 1))

    (defn get-users
      [db]
      (some->>
       (jdbc/plan db ["select account.id, account.email, role.name
                  from account
                  left join role on account.id = role.account_id"])
       reduce-user))

    (p (get-users db))

    ;; sql/

    (def user {:user/email "neil.figg+new@tenfren.com"
               :user/first-name "Neil",
               :user/last-name "Figg",
               :user/screen-name "figgy"
               :user/active true,
               :user/password (hashers/encrypt "12345678")
               :user/token-key "A4I774XAQM36J7IL"
               :user/created-timestamp (tick/date-time)
               :user/modified-timestamp (tick/date-time)})

    (sql/insert! db :account user)

    ;; postgresql we get the new record inserted
    ;; H2 #:user{:id 4}

    (def user {:user/screen-name "new screen name"})

    (sql/update! db :account
                 (dissoc user :user/id)
                 {:id 1})

    ;; #:next.jdbc{:update-count 1}

    (do
     ;; H2
      (def db-h2 {:jdbcUrl "jdbc:h2:file:./databases/tenfren-dev.db;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
                  :user    "sa"
                  :password ""})
      (def ds-h2 (jdbc/get-datasource db-h2))
      (def ds-h2 (jdbc/with-options ds-h2 jdbc/snake-kebab-opts))

      (sql/insert! ds-h2 :account user))))

(comment
  (do
    (require '[malli.generator :as mg])

    (def non-empty-string #"^\S*{1,10}$")
    (mg/generate non-empty-string)
    (m/validate non-empty-string "1 2")

    (def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")
    (mg/generate email-regex)

    (def email  [:re {:error/message "Not a valid email"}
                 #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$"])

    (mg/generate email)

    (def schema [:map
                 [:user/email email-regex]
                 [:user/password non-empty-string]])

    (mg/generate schema {:size 100})
    (mg/sample schema {:size 100})))

(comment
  (do
    (require '[clojure.data.codec.base64 :as base64])

    (defn encode-basic-authorization-token [user password]
      (new String (base64/encode (.getBytes (format "%s:%s" user password)))))

    (encode-basic-authorization-token "neil@tenfren.com" "12345678")

    ;; "bmVpbC5maWdnQGdtYWlsLmNvbToxMjM0NTY3OA=="
))

(comment
  (require
   '[tenfren.test-fixtures :as t-fix]
   '[tenfren.schema :as t-schema])

  (do
    (t-fix/gen-user {:schema t-schema/new-user :roles ["admin" "user"] :active? true :db? true})))
