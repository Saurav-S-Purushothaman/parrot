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
;; the console, to a file, to a database or over HTTP etc.  By keying
;; each appenders (:console, :file, :http) users can target specific
;; appenders when they want to remove or reconfigure them.
(defonce appenders
  (atom
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

(def ^:dynamic *context* {})

(defmacro with-context
  "Binds extra key/value pairs for all logs in the body"
  [context-map & body]
  `(binding [*context* (merge *context* ~context-map)]
     ;; After updating the context, execute the body
     ~@body))

;; NOTE: the point of having with context
;; 1. It is easy to propogate/layer context. Since we are merging the
;; context, it will allow us layer the context
;;   For eg. (with-context {:user 42} (with-context {:trace-id t .. }))

(defn file-appender
  "Return an appender that writes each event to `path`"
  [path]
  (let [writer (java.io.PrintWriter
                (java.io.BufferedWriter.
                 (java.io.FileWriter. path true)))]
    (fn [{:keys [level msg ns line context]}]
      (.println writer
                (str (java.time.Instant/now)
                     " " (name level)
                     " [" ns " : " line "]"
                     msg
                     (when (seq context)
                       (str " | " context))))
      (.flush writer))))

;; Logging macro
(defmacro log
  [level fmt & args]
  (let [lvl-ord (level-order level)
        curr-ord (level-order *level*)]
    (when (<= lvl-ord curr-ord)
      (let [fmeta (meta &form)]
       `(let [msg# (format ~fmt ~@args)
             file# *file*
             ns# *ns*
             event# {:level ~level
                     :msg   msg#
                     :ns    ns#
                     :file  file#
                     :line  (or (:line ~fmeta) 0)
                     :context *context*}]
         (doseq [ap# (vals @appenders)]
           (try (ap# event#)
                (catch Throwable _# nil))))))))


(defmacro trace [fmt & args] `(log :trace ~fmt ~@args))
(defmacro debug [fmt & args] `(log :debug ~fmt ~@args))
(defmacro info  [fmt & args] `(log :info  ~fmt ~@args))
(defmacro warn  [fmt & args] `(log :warn  ~fmt ~@args))
(defmacro error [fmt & args] `(log :error ~fmt ~@args))




(comment
  (binding [*level* :debug]
    (register-appender! :file (file-appender "app.log"))
    (with-context {:service "payment"
                   :env "prod"}
      (info "Service started")
      (debug "Config : %s" {:config :true})))
  nil)
