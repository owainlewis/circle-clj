(ns circle-clj.core
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

(def endpoint "https://circleci.com/api/v1")

(defn request
  "Runs a HTTP request to Circle CI API"
  ([method resource token query-params body]
    (let [underlying-request
           {:method method
            :url (str endpoint resource)
            :headers {"Accept" "application/json"}
            :body body
            :query-params
              (let [qp {:circle-token token}]
                (if (map? query-params)
                  (merge qp query-params)
                  qp))}]
    (let [[status resp] (try
                          (-> underlying-request
                              http/request
                              ((juxt :status :body)))
                        (catch Exception e
                          [nil (.getMessage e)]))]
      (if (= 200 status)
        (with-meta
          (json/parse-string resp true)
          {:operation (format "%s %s"
                        (.toUpperCase (name method)) resource)})
        {:error resp}))))
    ([method resource token]
      (request method resource token nil nil)))

(defn me
  "Provides information about the signed in user."
  [token]
  (request :get "/me" token))

(defn projects
  "List of all the projects you're following on CircleCI, with build information organized by branch."
  [token]
  (request :get "/projects" token))

(defn project-summary
  "Build summary for each of the last 30 builds for a single git repo"
  [token username project]
  (request :get 
    (format "/project/%s/%s" username project) token))

(defn recent-builds
  "Build summary for each of the last 30 recent builds, ordered by build_num"
  [token]
  (request :get "/recent-builds" token))

(defn build-details
  "GET: /project/:username/:project/:build_num
   Full details for a single build. The response includes all of the fields from the build summary.
   This is also the payload for the [notification webhooks](/docs/configuration/#notify),
   in which case this object is the value to a key named 'payload'"
  [token username project build]
  (request :get
    (format "/project/%s/%s/%s" username project (str build))
      token))

(defn list-artifacts
  "GET: /project/:username/:project/:build_num/artifacts
   List the artifacts produced by a given build."
  [token username project build]
  (request :get
    (format "/project/%s/%s/%s/artifacts" username project (str build))
    token))

(defn retry-build
  "POST: /project/:username/:project/:build_num/retry
   Retries the build, returns a summary of the new build."
  [token username project build]
  (request :post
    (format "/project/%s/%s/%s/retry" username project (str build))
    token))

;; POST: /project/:username/:project/:build_num/cancel
;; Cancels the build, returns a summary of the build.

(defn cancel-build
  [token username project build]
  (request :post
    (format "/project/%s/%s/%s/cancel" username project (str build))
      token))

;; POST: /project/:username/:project/:build_num/ssh-users
;; Adds a user to the build's SSH permissions.

(defn trigger-build
  "Triggers a new build, returns a summary of the build.
   [Optional build parameters can be set using an experimental API](/docs/parameterized-builds/)"
  [token username project branch & build-params]
  (request :post
    (format "/project/%s/%s/tree/%s" username project branch)
    token { } (into {} build-params)))

;; POST: /project/:username/:project/ssh-key
;; Create an ssh key used to access external systems that require SSH key-based authentication

(defn get-checkout-keys
  "Lists checkout keys"
  [token username project]
  (request :get
    (format "/project/%s/%s/checkout-key" username project)
    token))

;; POST: /project/:username/:project/checkout-key
;; Create a new checkout key.

(defn get-checkout-key
  "Get a checkout key"
  [token username project fingerprint]
  (request :get
    (format "/project/%s/%s/checkout-key/%s" username project fingerprint)
    token))

;; DELETE: /project/:username/:project/checkout-key/:fingerprint
;; Delete a checkout key.

;; DELETE: /project/:username/:project/build-cache
;; Clears the cache for a project.

;; POST: /user/ssh-key
;; Adds a CircleCI key to your GitHub User account.

;; POST: /user/heroku-key
;; Adds your Heroku API key to CircleCI, takes apikey as form param name.
