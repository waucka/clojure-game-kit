(ns clojure-game-kit.input
  (:import (org.lwjgl.input Keyboard Mouse))
  (:require [clojure-game-kit.util :refer :all])
  (:gen-class))

(defmacro def-level-trigger [name bindings default & rules]
  `(defn ~name [~'state key#]
     (let [~@bindings]
       (case+ key#
              ~@rules
              ((fn [~'key] ~default) key#)))))

(defmacro def-edge-trigger [name bindings default & rules]
  (let [rule-sets (partition 3 3 nil rules)
        key-state-sym (gensym "key-state")]
    (debug-println rule-sets)
    `(defn ~name [~'state key# ~key-state-sym]
       (let [~@bindings]
         (case+ key#
                ~@(mapcat (fn [rule-set]
                            (let [caseval (nth rule-set 0)
                                  rule1 (nth rule-set 1)
                                  rule2 (nth rule-set 2)]
                              (list caseval (list 'if key-state-sym rule1 rule2)))) rule-sets)
                ((fn [~'key ~'key-state] ~default) key# ~key-state-sym))))))

(defn get-keyboard-state [state-changes pressed-keys]
  (if (Keyboard/next)
    (let [key (Keyboard/getEventKey)
          key-state (Keyboard/getEventKeyState)]
      (recur (conj state-changes [key key-state])
             (if key-state
               (conj pressed-keys key)
               (disj pressed-keys key))))
    [state-changes pressed-keys]))

(defmacro def-level-trigger-input-loop [name level-trigger-function]
  `(defn ~name [state# pressed-keys#]
     (if (empty? pressed-keys#)
       state#
       (let [key# (first pressed-keys#)
             rest-of-list# (rest pressed-keys#)]
         (recur (~level-trigger-function state# key#) rest-of-list#)))))

(defmacro def-edge-trigger-input-loop [name edge-trigger-function]
  `(defn ~name [state# state-changes#]
     (if (empty? state-changes#)
       state#
       (let [state-change# (first state-changes#)
             key# (first state-change#)
             key-state# (second state-change#)
             rest-of-list# (rest state-changes#)]
         (recur (~edge-trigger-function state# key# key-state#) rest-of-list#)))))

(defn nil-level-trigger-loop [state _]
  state)

(defn nil-edge-trigger-loop [state _]
  state)

(defn get-mouse-state [state-changes pressed-buttons]
  (if (Mouse/next)
    (let [button (Mouse/getEventButton)
          button-state (Mouse/getEventButtonState)]
      (recur (conj state-changes [button button-state])
             (if button-state
               (conj pressed-buttons button)
               (disj pressed-buttons button))))
    (let [dx (Mouse/getDX)
          dy (Mouse/getDY)
          dwheel (Mouse/getDWheel)]
      [dx dy dwheel state-changes pressed-buttons])))

(defmacro def-mouse-function [name edge-trigger-loop level-trigger-loop motion-function]
  `(defn ~name [state#]
     (let [mouse-state# (get-mouse-state [] (:pressed-buttons state#))
           dx# (nth mouse-state# 0)
           dy# (nth mouse-state# 1)
           dwheel# (nth mouse-state# 2)
           state-changes# (nth mouse-state# 3)
           pressed-buttons# (nth mouse-state# 4)
           augmented-state# (assoc state# :pressed-buttons pressed-buttons#)]
       (debug-println pressed-buttons#)
       (~motion-function
        (~edge-trigger-loop
         (~level-trigger-loop augmented-state# pressed-buttons#)
         state-changes#)
        dx# dy# dwheel#))))

(defmacro def-process-input [name mouse-function edge-trigger-loop level-trigger-loop]
  `(defn ~name [state#]
     (let [keyboard-state# (get-keyboard-state [] (:pressed-keys state#))
           state-changes# (first keyboard-state#)
           pressed-keys# (second keyboard-state#)
           augmented-state# (assoc state# :pressed-keys pressed-keys#)]
       (debug-println pressed-keys#)
       (~mouse-function
        (~edge-trigger-loop
         (~level-trigger-loop augmented-state# pressed-keys#)
         state-changes#)))))
