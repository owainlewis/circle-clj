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
        (json/parse-string resp true)
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
