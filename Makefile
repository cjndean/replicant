node_modules:
	npm install

shadow: node_modules
	npx shadow-cljs watch app

backend:
	clj -M -m server.core

dev: node_modules
	npx shadow-cljs watch app & clj -M -m server.core

.PHONY: shadow backend dev

# To build and run your full application, you can now:
# Run just the frontend: make shadow
# Run just the backend: make backend
# Run both together: make dev