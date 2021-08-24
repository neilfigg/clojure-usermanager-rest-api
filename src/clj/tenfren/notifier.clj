(ns tenfren.notifier
  "Notifies the User of events. Currently uses emails, but could also be expanded to include SMS.
  To log, rather than email, change the dev `config.edn` to `:notifier-enabled false`.
  The test `config.edn` is already configured to log."
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [postal.core :as postal]
   [tick.alpha.api :as tick]))

(defn send-email-request
  "Send the email."
  [{:keys [host user password from]}
   {:keys [to subject plain html]}]
  (log/info "sending email request:" {:to to :subject subject})
  (postal/send-message
   {:host host
    :user user
    :pass password
    :ssl  true}
   {:from    from
    :to      to
    :subject subject
    :body    [:alternative
              {:type "text/plain" :content plain}
              {:type "text/html;charset=utf-8" :content html}]})
  (log/info "sent email request:" {:to to :subject subject}))

(defn log-email-request
  [{:keys [to subject]}]
  (log/info "logging email request:" {:to to :subject subject}))

;; The Emailer type used is determined in the system.clj.

(defprotocol Emailer
  (send! [config message]))

(defrecord SMTPEmailer [config]
  Emailer
  (send! [_ message] (send-email-request config message)))

(defrecord LogEmailer []
  Emailer
  (send! [_ message] (log-email-request message)))

(defn- send-email
  [emailer template subject email tokens]
  (.send! emailer  {:subject subject
                    :to      email
                    :plain   (-> template :text tokens)
                    :html    (-> template :html tokens)}))

(defn countdown-HH-mm-ss
  "Work out roughly how long until the token in the email expires."
  [expires-in-secs]
  (let [end-time (tick/>> (tick/now)
                          (tick/new-duration expires-in-secs :seconds))
        duration (tick/duration
                  {:tick/beginning (tick/instant)
                   :tick/end end-time})
        hours (tick/hours duration)
        minutes (tick/minutes (tick/- duration
                                      (tick/new-duration hours :hours)))
        seconds (tick/seconds (tick/- duration
                                      (tick/new-duration minutes :minutes)
                                      (tick/new-duration hours :hours)))]
    (cond
      (> hours 0)
      (format "%d hour(s)"  hours)
      (> minutes 0)
      (format "%d minute(s)" minutes)
      (> seconds 0)
      (format "%d second(s)" seconds)
      :else "The verification code has expired")))

(def templates
  {:registration
   {:subject "Tenfren Registration Request"
    :html    (slurp (io/resource "templates/tenfren-registration.html"))
    :text    (slurp (io/resource "templates/tenfren-registration.txt"))}
   :registration-confirmation
   {:subject "Tenfren Registration Confirmation"
    :html    (slurp (io/resource "templates/tenfren-registration-confirmation.html"))
    :text    (slurp (io/resource "templates/tenfren-registration-confirmation.txt"))}
   :password-reset
   {:subject "Tenfren Password Reset"
    :html    (slurp (io/resource "templates/tenfren-password-reset.html"))
    :text    (slurp (io/resource "templates/tenfren-password-reset.txt"))}})

(defn- salutation
  [first-name]
  (if (seq first-name) first-name""))

(defn registration-notification
  [emailer
   {:keys [user/email user/first-name]}
   {:keys [token expires-in-secs]}]
  (let [template (-> templates :registration)
        subject (-> template :subject)
        salutation (salutation first-name)
        tokens (fn [s] (-> s
                           (str/replace "{{name}}" salutation)
                           (str/replace "{{token}}" (str token))
                           (str/replace "{{expiry-period}}" (countdown-HH-mm-ss expires-in-secs))))]
    (send-email emailer template subject email tokens)))

(defn registration-confirmation-notification
  [emailer
   {:keys [user/email user/first-name]}]
  (let [template (-> templates :registration-confirmation)
        subject (-> template :subject)
        salutation (salutation first-name)
        tokens (fn [s] (-> s
                           (str/replace "{{name}}" salutation)))]
    (send-email emailer template subject email tokens)))

(defn password-reset-request-notification
  [emailer
   {:keys [user/email user/first-name]}
   {:keys [token expires-in-secs]}]
  (let [template (-> templates :password-reset)
        subject (-> template :subject)
        salutation (salutation first-name)
        tokens (fn [s] (-> s
                           (str/replace "{{name}}" salutation)
                           (str/replace "{{token}}" (str token))
                           (str/replace "{{expiry-period}}" (countdown-HH-mm-ss expires-in-secs))))]
    (send-email emailer template subject email tokens)))

(comment
  (do
    (def future 7200)
    (def future1 120)
    (def future2 59)
    (def past -1)
    (doseq [time [future future1 future2 past]]
      (println (countdown-HH-mm-ss time)))))
