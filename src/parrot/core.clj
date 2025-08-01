(ns parrot.core)

(defonce ^:private level-order
  {:trace 0
   :debug 1
   :info  2
   :warn  3
   :error 4})

(def ^{:doc "Dynamic level which can bind per thread or envelope"
       :dynamic true}
  *level* :info)

;; Now, appenders
;; NOTE: In logging originating in Log$j, an appender is simply a
;; component whose job is to append each log event somewhere. For eg. to
;; the console, to a file, to a database or over HTTP etc.
(defonce appenders
  (atom
   ;; NOTE: By keying each appenders (:console, :file, :http) users can
   ;; target specific appenders when they want to remove or reconfigure
   ;; them.
   {:console (fn [{:keys [level msg ns file line context]}]
               (println
                (str (name level)
                     " [ " ns ":" line "] "
                     msg
                     (when (seq context)
                       (str " | " context)))))}))

;; The following are the helper function to configure, reconfigure,
;; remove and add appenders.

(defn register-appender!
  "Add or replace an appendery by key."
  [k f]
  (swap! appenders assoc k f))

(defn remove-appender!
  "Remove an appender by key."
  [k]
  (swap! appenders dissoc k))
