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

CP_VERSION ?= 5.3.1
KAFKA_CONNECT_DATAGEN_VERSION ?= 0.1.5
AGGREGATE_VERSION = $(KAFKA_CONNECT_DATAGEN_VERSION)-$(CP_VERSION)

KAFKA_CONNECT_DATAGEN_LOCAL_VERSION = $(shell make local-package-version)
AGGREGATE_LOCAL_VERSION = $(KAFKA_CONNECT_DATAGEN_LOCAL_VERSION)-$(CP_VERSION)

help:
	@$(foreach m,$(MAKEFILE_LIST),grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(m) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-$(HELP_TAB_WIDTH)s\033[0m %s\n", $$1, $$2}';)

local-package-version: ## Retrieves the jar version from the maven project definition
	@mvn help:evaluate -Dexpression=project.version -q -DforceStdout

package: check-dependencies ## Creates the assembly jar
	@mvn clean package

build-local: ## Build the Docker image using the locally mvn built kafka-connect-datagen package
	@docker build -t kafka-connect-datagen:$(AGGREGATE_LOCAL_VERSION) --build-arg KAFKA_CONNECT_DATAGEN_VERSION=$(KAFKA_CONNECT_DATAGEN_LOCAL_VERSION) --build-arg CP_VERSION=$(CP_VERSION) -f Dockerfile .

build-cp-kafka-connect-confluenthub: ## Build the cp-kafka-connect image pulling datagen from Confluent Hub
	@docker build -t cnfldemos/kafka-connect-datagen:$(AGGREGATE_VERSION) --build-arg KAFKA_CONNECT_DATAGEN_VERSION=$(KAFKA_CONNECT_DATAGEN_VERSION) --build-arg CP_VERSION=$(CP_VERSION) -f Dockerfile-confluenthub .

build-cp-server-connect-operator-confluenthub: ## Build the cp-server-connect-operator image pulling datagen from Confluent Hub
	@docker build -t cnfldemos/cp-server-connect-operator-with-datagen:$(AGGREGATE_VERSION) --build-arg KAFKA_CONNECT_DATAGEN_VERSION=$(KAFKA_CONNECT_DATAGEN_VERSION) --build-arg CP_VERSION=$(CP_VERSION) -f Dockerfile-operator .

publish: build-cp-kafka-connect-confluenthub build-cp-server-connect-operator-confluenthub ## Builds and publishes all images to Dockerhub
	@docker push cnfldemos/kafka-connect-datagen:$(AGGREGATE_VERSION)
	@docker push cnfldemos/cp-server-connect-operator-with-datagen:$(AGGREGATE_VERSION)

