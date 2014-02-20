# ring-proxy

HTTP proxy [ring
middleware](https://github.com/ring-clojure/ring/blob/a02518275a06835e4fdd1a3af59d7c4c0408d25b/SPEC#L12)
for Clojure web applications.

## Usage

### Dependency

```clojure
[tailrecursion/ring-proxy "2.0.0-SNAPSHOT"]
```

### Example

Assuming your application's route handler is defined as `routes`, you
may add a proxied route with something like the following:

```clojure
(ns your-ns
  (:require [tailrecursion.ring-proxy :refer [wrap-proxy]]))
  
(def app
  (-> routes
      (wrap-proxy "/remote" "http://some.remote.server/remote")))
```

## License

Copyright © 2013 Alan Dipert and Micha Niskin

Distributed under the Eclipse Public License, the same as Clojure.
