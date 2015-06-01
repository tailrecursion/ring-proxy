(ns tailrecursion.ring-proxy
  (:require
    [clj-http.client         :refer [request]]
    [clojure.string          :refer [join split]]
    [ring.adapter.jetty      :refer [run-jetty]]
    [ring.middleware.cookies :refer [wrap-cookies]] )
  (:import (java.net URI)) )

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
  "Proxies requests from proxied-path, a local URI, to the remote URI at
  remote-base-uri, also a string."
  [handler ^String proxied-path remote-base-uri & [http-opts]]
  (wrap-cookies
   (fn [req]
     (if (.startsWith ^String (:uri req) proxied-path)
       (let [uri (URI. remote-base-uri)
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

(defn run-proxy
  [listen-path listen-port remote-uri http-opts]
  (-> (constantly {:status 404 :headers {} :body "404 - not found"})
      (wrap-proxy listen-path remote-uri http-opts)
      (run-jetty {:port listen-port}) ))
