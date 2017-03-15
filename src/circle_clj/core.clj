(ns circle-clj.core
  (:require [clj-http.client :as http]
            [clojure.string :as s]
            [cheshire.core :as json]))

(def ^:private endpoint "https://circleci.com/api/v1")

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
                  qp))}
          [status resp]
            (try
              (-> underlying-request
                  http/request
                 ((juxt :status :body)))
              (catch Exception e
              [nil (.getMessage e)]))]
      (if (or (= 200 status)
              (= 201 status))
        (with-meta
          (json/parse-string resp true)
          {:operation (format "%s %s"
                        (.toUpperCase (name method)) resource)})
        {:error resp})))
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
  (let [resource (path-builder "project" username project)]
    (request :get resource token query-params nil)))

(defn recent-builds
  "Build summary for each of the last 30 recent builds, ordered by build_num
   The follwing query params may also be added

   limit -> The number of builds to return. Maximum 100, defaults to 30.
   offset -> The API returns builds starting from this offset, defaults to 0.
  "
  [token query-params]
  (let [resource (path-builder "recent-builds")]
    (request :get resource token query-params nil)))

(defn build-details
  "GET: /project/:username/:project/:build_num
   Full details for a single build. The response includes all of the fields from the build summary.
   This is also the payload for the [notification webhooks](/docs/configuration/#notify),
   in which case this object is the value to a key named 'payload'"
  [token username project build]
  (let [resource (path-builder "project" username project build)]
    (request :get resource token)))

(defn list-artifacts
  "GET: /project/:username/:project/:build_num/artifacts
   List the artifacts produced by a given build."
  [token username project build]
  (let [resource (path-builder "project" username project build)]
    (request :get resource token)))

(defn retry-build
  "POST: /project/:username/:project/:build_num/retry
   Retries the build, returns a summary of the new build."
  [token username project build]
  (let [resource (path-builder "project" username project build)]
    (request :post resource token)))

(defn cancel-build
  "Cancels the build, returns a summary of the build"
  [token username project build]
  (let [resource (path-builder "project" username project build "cancel")]
    (request :post resource token)))

(defn add-ssh-user
  "Adds a user to the build's SSH permissions"
  [token username project build]
  (let [resource (path-builder "project" username project build)]
    (request :post resource token)))

(defn trigger-build
  "Triggers a new build, returns a summary of the build.
   [Optional build parameters can be set using an experimental API](/docs/parameterized-builds/)"
  [token username project branch]
  (let [resource (path-builder "project" username project "tree" branch)]
    (request :post resource token)))

(defn get-checkout-keys
  "Lists checkout keys"
  [token username project]
  (let [resource (path-builder "project" username project "checkout-key")]
    (request :get resource token)))

(defn get-checkout-key
  "Get a checkout key"
  [token username project fingerprint]
  (let [resource (path-builder "project" username project "checkout-key" fingerprint)]
    (request :get resource token)))

(defn list-env-vars
  "Lists the environment variables for :project"
  [token username project]
  (let [resource (path-builder "project" username project "envvar")]
    (request :get resource token)))

(defn add-env-var
  "Creates a new environment variables"
  [token username project data]
  (let [resource (path-builder "project" username project "envvar")]
    (request :post resource token nil data)))

(defn get-env-var
  "Gets the hidden value of environment variable :name"
  [token username project envvar]
  (let [resource (path-builder "project" username project "envvar" envvar)]
    (request :get resource token)))

(defn delete-env-var
  "Deletes the environment variable named ':name'"
  [token username project envvar]
  (let [resource (path-builder "project" username project "envvar" envvar)]
    (request :delete resource token)))
