(defproject clojure-game-kit "0.5.0"
  :description "An OpenGL/input framework in Clojure"
  :url "https://github.com/waucka/clojure-game-kit"
  :license {:name "LGPLv3+"
            :url "http://www.gnu.org/licenses/lgpl-3.0.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.lwjgl.lwjgl/lwjgl "2.9.1"]
                 [org.lwjgl.lwjgl/lwjgl_util "2.9.1"]
                 [org.impulse101/jimgload "1.0.0"]
                 ]
  :java-source-paths ["java/jobjloader"]
  :target-path "target/%s"
  :jar-exclusions [#"^.*demo.*"]
  :profiles {:uberjar {:aot :all}
             :dev {:main ^:skip-aot clojure-game-kit.demo}
             :linux {:dependencies [[org.lwjgl.lwjgl/lwjgl-platform "2.9.1" :classifier "natives-linux" :packaging "pom"]]}
             :windows {:dependencies [[org.lwjgl.lwjgl/lwjgl-platform "2.9.1" :classifier "natives-windows" :packaging "pom"]]}
             :osx {:dependencies [[org.lwjgl.lwjgl/lwjgl-platform "2.9.1" :classifier "natives-osx" :packaging "pom"]]}
             })
