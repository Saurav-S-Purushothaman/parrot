(ns parrot.core
  (:gen-class))

(defonce ^:private level-order
  {:trace 0
   :debug 1
   :info  2
   :warn  3
   :error 4})

(def ^{:doc "Dynamic level which can bind per thread or envelope"
       :dynamic true}
  *level* :info)

;; NOTE: In logging originating in Log$j, an appender is simply a
;; component whose job is to append each log event somewhere. For eg. to
;; the console, to a file, to a database or over HTTP etc.  By keying
;; each appenders (:console, :file, :http) users can target specific
;; appenders when they want to remove or reconfigure them. We are
;; following the same format for our logging library too.

(defn file-appender
  "Return an appender that writes each event to `path`"
  [path]
  (let [writer (java.io.PrintWriter
                (java.io.BufferedWriter.
                 (java.io.FileWriter. path true)))]
    (fn [{:keys [level msg ns line context]}]
      (.println writer
                (let [logs {:time (str (java.time.Instant/now))
                            :ns ns
                            :level level
                            :message msg}]
                  (if (seq context)
                    (str
                     (assoc logs :context context))
                    (str logs))))
      (.flush writer))))

(defn std-out-appender
  "Returns an appender that writes logs to the std-out"
  []
  (fn [{:keys [level msg ns file line context]}]
    (let [log-meta {:level level
                    :at (str "[" ns ":" line"]")
                    :namespact ns
                    :line-number line}]
      (clojure.pprint/pprint (if (seq context)
                               (assoc log-meta :context context)
                               log-meta)))))


(defonce ^"An atom to store all the appenders"
  appenders
  ;; For now, we can store only the std out appender as the default
  ;; value. Users has the ability to add the rest of the appenders by
  ;; themeself.
  (atom {:console (std-out-appender)}))

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
     ~@body))

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

(defn parrot-init
  "Configure the logging from an EDN or env-vars. Call once at startup."
  [{:keys [level file-path service env]}]
  (alter-var-root #'parrot.core/*level* (constantly level))
  (when file-path
    (register-appender! :file (file-appender file-path)))
  (alter-var-root #'parrot.core/*context*
                  (constantly {:service service
                               :env env})))


(comment
  (binding [*level* :debug]
    #_(register-appender! :file (file-appender "app.log"))
    (with-context {:service "payment"
                   :env "prod"}

      (with-context {:service "payment 2"
                     :env "prod2"}
        (with-context {:server-name "payment3"
                       :env "prod"}
         (info "Service started")))
      (debug "Config : %s" {:config :true})))
  nil)
