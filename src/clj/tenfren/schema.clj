(ns tenfren.schema
  (:require
   [malli.util :as mu]
   [sci.core]))

;; types

(def email [:re {:gen/fmap '(fn [_] (str (rand-int 10000) "@example.com"))
                 :error/message "Not a valid email"}
            #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$"])

(def password [:re {:gen/fmap '(fn [_] (str (rand-int 10000) "-password"))
                    :error/message "Not a valid password. Should be between 8 - 16 characters, and contain no spaces."}
               #"^[^\s]{8,16}$"]) ;; no spaces, any other char

(def otp pos-int?)

;;; schemas

(def message
  [:map
   [:message string?]])

(def user
  [:map
   [:user/id pos-int?]
   [:user/email email]
   [:user/password password]
   [:user/first-name [:maybe string?]]
   [:user/last-name [:maybe string?]]
   [:user/screen-name [:maybe string?]]
   [:user/roles [:vector any?]]
   [:user/last-login-timestamp [:maybe string?]]
   [:user/active boolean?]
   [:user/token-key string?]
   [:user/token [:maybe string?]]
   [:user/created-timestamp any?]
   [:user/created-user [:maybe string?]]
   [:user/modified-timestamp any?]
   [:user/modified-user [:maybe string?]]])

(def new-user
  (mu/select-keys
   user
   [:user/email
    :user/password
    :user/first-name
    :user/last-name
    :user/screen-name]))

(def user-summary
  (mu/select-keys
   user
   [:user/id
    :user/email
    :user/first-name
    :user/last-name
    :user/screen-name
    :user/roles
    :user/active]))

(def user-summary-token
  (-> user-summary
      (mu/assoc :user/token string?)))

(def user-summaries
  [:vector
   user-summary])

(def user-details
  (mu/select-keys
   user
   [:user/first-name
    :user/last-name
    :user/screen-name]))

(def user-status
  (mu/select-keys
   user
   [:user/email
    :user/active]))

(def user-status-patch
  (mu/select-keys
   user
   [:user/active]))

(def user-roles
  (mu/select-keys
   user
   [:user/email
    :user/roles]))

(def auth-header
  [:map
   [:authorization string?]])

(def registration
  new-user)

(def registration-confirmation
  [:map
   [:email email]
   [:verification-code otp]])

(def password-reset-request
  [:map
   [:email email]])

(def password-reset
  [:map
   [:email email]
   [:password password]
   [:verification-code otp]])

(def password-change
  [:map
   [:old-password password]
   [:new-password password]])

(def user-email
  [:map
   [:email email]])
