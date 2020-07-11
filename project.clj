(defproject generator "0.1.0-SNAPSHOT"
  :description "FIXME: generates sample docs for network training"
  :plugins [[cider/cider-nrepl "0.24.0"][hellonico/lein-gorilla "0.4.2"][lein-auto "0.1.3"][lein-jupyter "0.1.16"]]
  :auto {:default {:file-pattern #"\.(clj)$"}}
  :repositories [["vendredi" "https://repository.hellonico.info/repository/hellonico/"]]
  :url "http://example.com/FIXME"
  :profiles {:dev {
    :resource-paths ["resources"]
    :dependencies [
    ; used for proto repl
    [org.clojure/tools.nrepl "0.2.11"]
    ; proto repl
    [proto-repl "0.3.1"]
    ; use to start a gorilla repl
    [seesaw "1.4.5"]]
    }}
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.cli "0.3.5"]
                 [origami "4.1.2"]
                 [org.clojure/data.json "0.2.7"]
                 [org.clojure/tools.logging "1.1.0"]
                 ; No need to specify slf4j-api, it’s required by logback
                 [ch.qos.logback/logback-classic "1.2.3"]
                 ]
  :exclusions
  [;; Exclude transitive dependencies on all other logging
   ;; implementations, including other SLF4J bridges.
   commons-logging
   log4j
   org.apache.logging.log4j/log4j
   org.slf4j/simple
   org.slf4j/slf4j-jcl
   org.slf4j/slf4j-nop
   org.slf4j/slf4j-log4j12
   org.slf4j/slf4j-log4j13
   ]
  :repl-options {:init-ns generator.core})
