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
          (rename-keys {:required? :required
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

(ns-unmap *ns* 'subs)
(def subs
  "A set of option subcommand types."
  (set (map command-option-types [:sub-command :sub-command-group])))

(defn sub?
  "Checks if option `opt` is a subcommand or subcommand group."
  [opt]
  (boolean (subs (:type opt))))

(defn option
  "Returns the first option from `options` with the name `name`."
  [name options]
  (first (filter (comp (partial = name) :name) options)))

(defn find-option
  "Looks for the commands of the interaction and collection provided, returning a map of `:in` and `:out` for each part.
  `:in` represents the interaction option while `:out` represents a command from the `options` collection."
  [{[nopt] :options
    :as opt} options]
  (if-let [o (option (:name opt) options)]
    (if (sub? nopt)
      (recur nopt (:options o))
      {:in opt
       :out o})))

(defn find-command
  "Looks for the command associated with the interaction (checking subcommands), returning `nil` if not found."
  [{:keys [data]} commands]
  (if (sub? data)
    (find-option data commands)))

(defn runner
  "Returns the function associated with the interaction. Currently distinguishes between regular commands and
  autocompletion."
  [{:keys [in out]}]
  (if-let [opt (first (filter :focused (:options in)))]
    (:autocomplete (get (:options out) (:name opt)))
    (:fn out)))

;;; Convenience

(defn mapify
  "Takes a collection of options and returns a map of them mapped by their names."
  [options]
  (reduce #(assoc %1 (:name %2) %2) {} options))

(defn component?
  "Returns a boolean indicating whether or not the interaction originates from a component."
  [inter]
  (boolean (:message inter)))
