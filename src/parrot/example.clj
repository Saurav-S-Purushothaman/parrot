(ns parrot.example
  (:require [parrot.core :as log :refer [parrot-init]]))

(def parrot-conf {:level :info
                  :service "example"
                  :env "local"})

(parrot-init parrot-conf)

(log/info "Hello world")
