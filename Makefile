.PHONY: *

HELP_TAB_WIDTH = 25

.DEFAULT_GOAL := help

SHELL=/bin/bash -o pipefail

check-dependency = $(if $(shell command -v $(1)),,$(error Make sure $(1) is installed))

check-dependencies:
	@#(call check-dependency,mvn)
	@#(call check-dependency,docker)
	@#(call check-dependency,grep)
	@#(call check-dependency,cut)
	@#(call check-dependency,sed)

help:
	@$(foreach m,$(MAKEFILE_LIST),grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(m) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-$(HELP_TAB_WIDTH)s\033[0m %s\n", $$1, $$2}';)

version-connector:
	@grep COPY Dockerfile-dockerhub | cut -d':' -f2 | sed 's/.*datagen-\(.*\)\.zip/\1/'

version-connect: check-dependencies
	@grep FROM Dockerfile-dockerhub | cut -d':' -f2

package: check-dependencies ## Creates any package artifacts (Docker, Assembly JAR, etc..)
	@mvn clean package

publish: package ## Publishes packages to registries (Docker only for now) 
	docker build . -f Dockerfile-dockerhub -t cnfldemos/kafka-connect-datagen:$(shell make version-connector)-$(shell make version-connect)
	docker push cnfldemos/kafka-connect-datagen:$(shell make version-connector)-$(shell make version-connect)
