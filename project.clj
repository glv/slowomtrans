(defproject slowomtrans "0.1.0-SNAPSHOT"
  :description "demonstration of a performance issue with om next"
  :url "http://github.com/glv/slowomtrans"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main ^:skip-aot slowomtrans.core
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [org.clojure/tools.cli "0.3.3"]
                 [org.clojure/core.async "0.2.374"]
                 [org.omcljs/om "1.0.0-alpha24"]
                 [figwheel-sidecar "0.5.0-SNAPSHOT" :scope "test"]]
  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-figwheel "0.5.0-2"]]
  :source-paths ["src"]
  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]
  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src"]

                        :figwheel {:on-jsload "slowomtrans.core/on-js-reload"}

                        :compiler {:main slowomtrans.core
                                   :asset-path "js/compiled/out"
                                   :output-to "resources/public/js/compiled/slowomtrans.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :source-map-timestamp true }}
                       {:id "min"
                        :source-paths ["src"]
                        :compiler {:output-to "resources/public/js/compiled/slowomtrans.js"
                                   :main slowomtrans.core
                                   :optimizations :advanced
                                   :pretty-print false}}]}
  :figwheel {:server-port 3450
             :css-dirs ["resources/public/css"] ;; watch and update CSS
             })
