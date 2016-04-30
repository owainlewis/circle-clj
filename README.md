# Circle

[![Circle CI](https://circleci.com/gh/owainlewis/circle-clj.svg?style=svg)](https://circleci.com/gh/owainlewis/circle-clj)

A Clojure client for Circle CI API

## Usage

All operations require an API token which can be found in your Circle CI account settings

```clojure
(ns myproject.core
  (:require [circle-clj :as circle]))

(def token "XYZ")

;; Your profile

(cirle/me token)

;; Your projects

(circle/projects token)

(->> (projects token) first :reponame)

;; Trigger a build

(circle/trigger-build token "owainlewis" "circle-clj" "master")
```
## License

Copyright © 2016 Owain Lewis

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
