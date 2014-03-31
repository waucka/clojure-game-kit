(ns clojure-game-kit.util
  (:import (java.nio.file Path FileSystems Files)
           (java.nio.charset StandardCharsets)
           (java.io FileInputStream InputStreamReader BufferedReader)
           (org.lwjgl Sys)
           (org.lwjgl.util.glu Registry GLU)
           (org.lwjgl.util.vector Matrix4f Vector3f))
  (:gen-class))

(def global-debug (atom false))

(defn set-global-debug [on-or-off]
  (swap! global-debug (fn [_] on-or-off)))

(defmacro if-debug [& body]
  `(when @global-debug ~@body))

(defmacro debug-println [& message]
  `(when @global-debug (println ~@message)))

(defn make-path [path]
  (.getPath (FileSystems/getDefault) path (into-array [""])))

(defn join-paths [path1 path2]
  (.getPath (FileSystems/getDefault) path1 (into-array [path2])))

(defn open-file [path]
  (Files/newBufferedReader path StandardCharsets/UTF_8))

(defn get-time []
  (/ (* 1000 (Sys/getTime)) (Sys/getTimerResolution)))

(defn require-extensions [& extensions]
  (let [reg (Registry.)
        all-extensions (.gluGetString GLU/GLU_EXTENSIONS)]
    (doseq [ext extensions]
      (when (not (.gluCheckExtension ext all-extensions))
        (throw (Exception. (str ext " is not available, but we need it!")))))))

(defn cotangent [angle]
  (/ 1.0 (Math/tan angle)))

(defn deg-to-rad [deg]
  (* deg (/ Math/PI 180.0)))

(defn make-matrix4f [e00 e01 e02 e03
                     e10 e11 e12 e13
                     e20 e21 e22 e23
                     e30 e31 e32 e33]
  (let [mat (Matrix4f.)]
    ;COLUMN-MAJOR ORDER?  WTF?
    (set! (. mat m00) e00)
    (set! (. mat m01) e10)
    (set! (. mat m02) e20)
    (set! (. mat m03) e30)
    (set! (. mat m10) e01)
    (set! (. mat m11) e11)
    (set! (. mat m12) e21)
    (set! (. mat m13) e31)
    (set! (. mat m20) e02)
    (set! (. mat m21) e12)
    (set! (. mat m22) e22)
    (set! (. mat m23) e32)
    (set! (. mat m30) e03)
    (set! (. mat m31) e13)
    (set! (. mat m32) e23)
    (set! (. mat m33) e33)
    mat))

(defn make-projection-matrix [fov width height near-clip far-clip]
  (let [aspect-ratio (/ width height)
        y-scale (cotangent (deg-to-rad (/ fov 2.0)))
        x-scale (/ y-scale aspect-ratio)
        frustum-length (- far-clip near-clip)
        ;Column-major order!  ARGH!
        m22 (- (/ (+ far-clip near-clip) frustum-length))
        m32 (- (/ (* 2 near-clip far-clip) frustum-length))]
    (make-matrix4f x-scale 0 0 0
                   0 y-scale 0 0
                   0 0 m22 m32
                   0 0 -1 0)))

(defn translate-matrix [mat delta]
  (let [tmat (.translate (Matrix4f.) delta)]
    (debug-println mat "trans" delta "=" (Matrix4f/mul tmat mat nil))
    (Matrix4f/mul tmat mat nil)))

(defn rotate-matrix [mat angle axis]
  (let [tmat (.rotate (Matrix4f.) angle axis)]
    (debug-println mat "rot" angle "," axis "=" (Matrix4f/mul tmat mat nil))
    (Matrix4f/mul tmat mat nil)))

(defn scale-matrix [mat factors]
  (let [tmat (.scale (Matrix4f.) factors)]
    (debug-println mat "scale" factors "=" (Matrix4f/mul tmat mat nil))
    (Matrix4f/mul tmat mat nil)))

(defn negate-vector3f [vector]
  (let [x (. vector x)
        y (. vector y)
        z (. vector z)]
    (Vector3f. (- x) (- y) (- z))))

(defmacro case+
  "Same as case, but evaluates dispatch values, needed for referring to
   class and def'ed constants as well as java.util.Enum instances."
  [value & clauses]
  (let [clauses (partition 2 2 nil clauses)
        default (when (-> clauses last count (== 1))
                  (last clauses))
        clauses (if default (drop-last clauses) clauses)
        eval-dispatch (fn [d]
                        (if (list? d)
                          (map eval d)
                          (eval d)))]
    `(case ~value
       ~@(concat (->> clauses
                   (map #(-> % first eval-dispatch (list (second %))))
                   (mapcat identity))
           default))))

(defn open-file-for-reading [filename]
  (BufferedReader. (InputStreamReader. (FileInputStream. filename))))
