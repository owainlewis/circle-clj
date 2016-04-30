(ns circle-clj.core
  (:require [clj-http.client :as http]
            [clojure.string :as s]
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

(defn path-builder
  "Utility method for constructing resource paths"
  [& parts]
  (let [normalized-parts (map str parts) ;; handle any numeric values
        joined (s/join "/" normalized-parts)]
    (str "/" joined)))

(defn me
  "Provides information about the signed in user."
  [token]
  (request :get (path-builder "me") token))

(defn projects
  "List of all the projects you're following on CircleCI, with build information organized by branch."
  [token]
  (request :get (path-builder "projects") token))

(defn project-summary
  "Build summary for each of the last 30 builds for a single git repo.
   The following query params may also be added:
     limit -> The number of builds to return. Maximum 100, defaults to 30.
     offset -> The API returns builds starting from this offset, defaults to 0.
     filter -> Restricts which builds are returned. Set to completed, successful, failed, running, or defaults to no filter
  "
  [token username project query-params]
  (request :get (path-builder "project" username project) token query-params nil))

(defn recent-builds
  "Build summary for each of the last 30 recent builds, ordered by build_num
   The follwing query params may also be added

   limit -> The number of builds to return. Maximum 100, defaults to 30.
   offset -> The API returns builds starting from this offset, defaults to 0.
  "
  [token query-params]
  (request :get "/recent-builds" token query-params nil))

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

(defn add-ssh-user
  "Adds a user to the build's SSH permissions"
  [token username project build]
    (request :post
    (format "/project/%s/%s/%s/ssh-users" username project (str build))
      token))

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

(defn list-env-vars
  "Lists the environment variables for :project"
  [token username project]
  (request :get
    (format "/project/%s/%s/envvar" username project) token))

(defn add-env-var
  "Creates a new environment variables"
  [token username project data]
    (request :post
      (format "/project/%s/%s/envvar" username project) token nil data))

(defn get-env-var
  "Gets the hidden value of environment variable :name"
  [token username project envvar]
  (request :get
    (format "/project/%s/%s/envvar/%s" username project envvar) token))

(defn delete-env-var
  "Deletes the environment variable named ':name'"
  [token username project envvar]
  (request :delete
    (format "/project/%s/%s/envvar/%s" username project envvar) token))
