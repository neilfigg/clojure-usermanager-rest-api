(ns seed-db
  (:require
   [buddy.hashers :as hashers]
   [clojure.pprint :as pp :refer [pprint] :rename {pprint p}]
   [clojure.tools.logging :as log]
   [next.jdbc :as jdbc]
   [tenfren.config :as t-config]
   [tenfren.core :as t-core]
   [tenfren.db :as t-db]
   [tenfren.system :as system])
  (:gen-class))

;; admin is a User with an Admin role
;; user is a User with a User role

(defn add-user
  [db user]
  (jdbc/with-transaction [tx db]
    (let [tx-opts (t-db/default-ds-options tx)]
      (t-db/save-user tx-opts user))))

(def users
  [{:user/email "neil@tenfren.com"
    :user/first-name "Neil",
    :user/last-name "Tenfren",
    :user/screen-name "Neil!"
    :user/roles ["admin" "user"]
    :user/active true,
    :user/password (hashers/encrypt "12345678")
    :user/token-key "A4I774XAQM36J7IL"}
   {:user/email "loredana@tenfren.com"
    :user/first-name "Loredana",
    :user/last-name "Tenfren",
    :user/screen-name "Loredana!"
    :user/roles ["user"]
    :user/active true,
    :user/password (hashers/encrypt "12345678")
    :user/token-key "43EEW457BGHGIRJF"}])

(defn seed-users
  [db]
  (log/info "Seeding default users 'admin' and 'user'")
  (doseq [user users]
    (add-user db user))
  (log/info "Seeding done"))

(defn -main [& args]
  (let [db-config (select-keys t-config/default-config [:tenfren/db])
        _ (log/info "db config:" db-config)
        system (system/start-system! db-config)
        db     (-> system
                   :tenfren/db
                   t-db/default-ds-options)]
    (try
      (t-db/create-db db)
      (seed-users db)
      (p (t-core/get-users db))
      (finally (system/stop-system! system)))))

(comment
  (-main))

;; clj -M:dev:seed-db or clj -M:dev:h2:seed-db
