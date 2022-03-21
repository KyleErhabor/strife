(ns strife.core
  (:require
    [clojure.set :refer [rename-keys]]
    [discljord.messaging.specs :refer [command-option-types]]))

(defn transform-options
  "Transforms `options` into a structure compatible with Discord."
  [options]
  (map #(-> %
          (dissoc :fn)
          (update :autocomplete boolean)
          (rename-keys {:required? :rqeuired
                        :min :min_value
                        :max :max_value
                        :channels :channel-types})
          (update :options transform-options)) options))

(defn transform
  "Transforms `commands` into a structure compatible with Discord."
  [commands]
  (map #(-> %
          (dissoc :fn)
          (update :options transform-options)) commands))

(defn option
  "Returns the first option from `options` with the name `name`."
  [name options]
  (first (filter (comp (partial = name) :name) options)))

(def subs
  "A set of option subcommand types."
  (set (map command-option-types [:sub-command :sub-command-group])))

(defn sub?
  "Checks if option `opt` is a subcommand or subcommand group."
  [opt]
  (subs (:type opt)))

(defn find-option [opt options]
  (let [o (option (:name opt) options)
        next (first (:options opt))]
    (if (sub? next)
      (recur next (:options o))
      o)))

(defn find-command [{:keys [data]} commands]
  (if (sub? data)
    (find-command data commands)))

(defn run-command [command]
  (:fn command))
