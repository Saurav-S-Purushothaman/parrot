# Parrot
Parrot is a very simple logging library for Clojur where logs are
printed as Clojure maps

## Scope
Parrot is my educational project to learn more about logging and how it
works. Therefore all the features in this projects are going to be very
basic and won't be good enough for a production level logging system. I
hope to make this as polished as other logging library in Clojure like
`timbre` and `pedestal` logging. Even though this is a an eductional
project, I would love to get your feedback on the quality of the code
and library as PR or Issue or by mailing to `saurav.kudajadri@gmail.com`


## Usage:

Basic logging with default config

``` clojure
user=> (require '[parrot.core :as log])
user=> (log/info "Hello world")
{:message "Hello world",
 :level :info,
 :at "[user:113]",
 :namespace "user",
 :namespace2 "user",
 :line-number 113}
```

If you want to update the config, you can do that by providing the
parrot config and updating it via the `parrot-inti` function which is
just a one time activity

``` clojure
(def parrot-conf {:level :info
                  :service "example"
                  :env "local"})

(parrot-init parrot-conf)

(log/info "Hello world")
```

You can provide the propogating context while logging using the
`with-context` macro

``` clojure
(require '[parrot.core :as log :refer [with-context]])
```

To add simple context,

``` clojure
user> (with-context {:service "payment"
                     :env "prod"}
        (log/info "Hello world"))

{:message "Hello world",
 :level :info,
 :at "[user:113]",
 :namespace "user",
 :namespace2 "user",
 :line-number 113,
 :context {:service "payment", :env "prod"}}
nil
```

Context can be propagated. You can write nested contexts

``` clojure

user> (with-context {:service "payment"
                     :env "prod"}
        (with-context {:service "payment-double"}
          (log/info "Hello world")))

{:message "Hello world",
 :level :info,
 :at "[user:113]",
 :namespace "user",
 :namespace2 "user",
 :line-number 113,
 :context {:service "payment-double", :env "prod"}}
nil
```
