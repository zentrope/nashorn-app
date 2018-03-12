#
#
#
.PHONY: clean run deps updates help prodeps watch build clean-client
.PHONY: build-client-prod

.DEFAULT_GOAL := help

rlwrap = $(shell which rlwrap)

clean-client:
	rm -rf resources/public/out
	rm -f resource/public/main.js
	rm -f figwheel_server.log

watch: clean-client ## Watch the js/app build for changes and re-compile
	lein cljsbuild auto dev || true

deps: ## List dependencies and transitive dependencies
	lein deps :tree || true

prodeps: ## List dependencies for a production build
	lein with-profile production deps :tree || true

updates: ## Search for available updates to dependencies
	lein ancient :all :check-clojure :plugins :allow-qualified || true

run: ## Run the application
	lein trampoline run || true

repl: ## Start the NREPL server-side REPL
	lein repl

rebl: ## Start a fancy server-side REPL
	lein trampoline run -m rebel-readline.main || true

clean: ## Clean build artifacts
	lein clean

build-client-prod: clean-client ## Build the production client
	lein cljsbuild once client

build: clean build-client-prod ## Build a runnable app (uberjar)
	lein uberjar
	rm -rf target/classes target/cljsbuild* target/stale
	find target -type d -delete
	rm target/*classes.jar

figwheel: clean-client ## Start figwheel plugin for working with CLJS
	$(rlwrap) lein figwheel || true

db-console: ## Start a db-console web app (for h2).
	java -jar $(shell find ~/.m2 -iname "h2*jar" | sort | tail -1) || true

db-reset: ## Reset (really, delete) the database (h2).
	rm -rf ~/.kfi/scripto_app/storage.mv.db

help: ## Show makefile based help
	@grep -E '^[a-zA-Z0-9_-]+:.*?## .*$$' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-25s\033[0m %s\n", $$1, $$2}' \
		| sort
