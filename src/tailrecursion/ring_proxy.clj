(ns tailrecursion.ring-proxy
  (:require [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.adapter.jetty      :refer [run-jetty]]
            [clojure.string          :refer [join split]]
            [clj-http.client         :refer [request]])
  (:import (java.net URI)))

(defn prepare-cookies
  "Removes the :domain and :secure keys and converts the :expires key (a Date)
  to a string in the ring response map resp. Returns resp with cookies properly
  munged."
  [resp]
  (let [prepare #(-> (update-in % [1 :expires] str)
                     (update-in [1] dissoc :domain :secure))]
    (assoc resp :cookies (into {} (map prepare (:cookies resp))))))

(defn slurp-binary
  "Reads len bytes from InputStream is and returns a byte array."
  [^java.io.InputStream is len]
  (with-open [rdr is]
    (let [buf (byte-array len)]
      (.read rdr buf)
      buf)))

(defn wrap-proxy
  "Proxies requests to proxied-path, a local URI, to the remote URI at
  remote-uri-base, also a string."
  [handler ^String proxied-path remote-uri-base & [http-opts]]
  (wrap-cookies
   (fn [req]
     (if (.startsWith ^String (:uri req) (str proxied-path "/"))
       (let [uri (URI. remote-uri-base)
             remote-uri (URI. (.getScheme uri)
                              (.getAuthority uri)
                              (str (.getPath uri)
                                   (subs (:uri req) (.length proxied-path)))
                              nil
                              nil)]
         (-> (merge {:method (:request-method req)
                     :url (str remote-uri "?" (:query-string req))
                     :headers (dissoc (:headers req) "host" "content-length")
                     :body (if-let [len (get-in req [:headers "content-length"])]
                             (slurp-binary (:body req) (Integer/parseInt len)))
                     :follow-redirects true
                     :throw-exceptions false
                     :as :stream} http-opts)
             request
             prepare-cookies))
       (handler req)))))

(defn local-proxy-server
  [listen-port remote-uri-base http-opts]
  (->
    (constantly {:status 404 :headers {} :body "404 - not found"})
    (wrap-proxy "" remote-uri-base http-opts)
    (run-jetty {:port listen-port})))

(defn -main
  [listen-port remote-uri-base & opts]
  (local-proxy-server
    (read-string listen-port)
    remote-uri-base
    (if-not (seq opts) {} (read-string (join " " opts)))))
