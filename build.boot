(set-env!
  :resource-paths #{"src"}
  :dependencies   '[[org.clojure/clojure     "1.6.0"  :scope "provided"]
                    [adzerk/bootlaces        "0.1.10" :scope "test"]
                    [ring/ring-core          "1.4.0-RC1"]
                    [ring/ring-jetty-adapter "1.4.0-RC1"]
                    [clj-http                "1.1.2"] ])

(require '[adzerk.bootlaces :refer :all])

(def +version+ "3.0.0-SNAPSHOT")

(task-options!
  pom  {:project     'tailrecursion/ring-proxy
        :version     +version+
        :description "HTTP proxy ring middleware for Clojure web applications."
        :scm         {:url "https://github.com/tailrecursion/ring-proxy"}
        :license     {"EPL" "http://www.eclipse.org/legal/epl-v10.html"} })