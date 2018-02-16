#
#
#
.PHONY: clean run deps updates help prodeps

.DEFAULT_GOAL := help

deps: ## List dependencies and transitive dependencies
	lein deps :tree || true

prodeps: ## List dependencies for a production build
	lein with-profile production deps :tree || true

updates: ## Search for available updates to dependencies
	lein ancient :all :check-clojure :plugins :allow-qualified || true

run: ## Run the application
	lein trampoline run || true

clean: ## Clean build artifacts
	lein clean

help: ## Show makefile based help
	@grep -E '^[a-zA-Z0-9_-]+:.*?## .*$$' $(MAKEFILE_LIST) \
		| awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-25s\033[0m %s\n", $$1, $$2}' \
		| sort
