.PHONY: *

HELP_TAB_WIDTH = 25

.DEFAULT_GOAL := help

SHELL=/bin/bash -o pipefail

check-dependency = $(if $(shell command -v $(1)),,$(error Make sure $(1) is installed))

CP_VERSION ?= 6.1.0
OPERATOR_VERSION ?= 0

KAFKA_CONNECT_DATAGEN_VERSION ?= 0.4.0
AGGREGATE_VERSION = $(KAFKA_CONNECT_DATAGEN_VERSION)-$(CP_VERSION)
OPERATOR_AGGREGATE_VERSION = $(AGGREGATE_VERSION).$(OPERATOR_VERSION)

KAFKA_CONNECT_DATAGEN_LOCAL_VERSION = $(shell make local-package-version)
AGGREGATE_LOCAL_VERSION = $(KAFKA_CONNECT_DATAGEN_LOCAL_VERSION)-$(CP_VERSION)

BASE_PREFIX ?= confluentinc
PUSH_PREFIX ?= cnfldemos

help:
	@$(foreach m,$(MAKEFILE_LIST),grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(m) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-$(HELP_TAB_WIDTH)s\033[0m %s\n", $$1, $$2}';)

local-package-version: ## Retrieves the jar version from the maven project definition
	@mvn help:evaluate -Dexpression=project.version -q -DforceStdout

package: ## Creates the assembly jar
	@mvn clean package

build-docker-from-local: package ## Build the Docker image using the locally mvn built kafka-connect-datagen package
	@docker build -t kafka-connect-datagen:$(AGGREGATE_LOCAL_VERSION) --build-arg BASE_PREFIX=$(BASE_PREFIX) --build-arg KAFKA_CONNECT_DATAGEN_VERSION=$(KAFKA_CONNECT_DATAGEN_LOCAL_VERSION) --build-arg CP_VERSION=$(CP_VERSION) -f Dockerfile-local .

build-docker-from-released: ## Build a Docker image using a released version of the kafka-connect-datagen connector 
	@docker build -t kafka-connect-datagen:$(AGGREGATE_VERSION) --build-arg BASE_PREFIX=$(BASE_PREFIX) --build-arg KAFKA_CONNECT_DATAGEN_VERSION=$(KAFKA_CONNECT_DATAGEN_VERSION) --build-arg CP_VERSION=$(CP_VERSION) -f Dockerfile-confluenthub .

build-cp-server-connect-from-local: package ## Build the Docker image based on cp-server-connect from locally mvn built kafka-connect-datagen package
	@docker build -t cp-server-connect-datagen:$(AGGREGATE_LOCAL_VERSION) --build-arg BASE_PREFIX=$(BASE_PREFIX) --build-arg KAFKA_CONNECT_DATAGEN_VERSION=$(KAFKA_CONNECT_DATAGEN_LOCAL_VERSION) --build-arg CP_VERSION=$(CP_VERSION) --build-arg CONNECT_IMAGE=cp-server-connect -f Dockerfile-local .

build-cp-server-connect-from-released: ## Build a Docker image using a released version of the kafka-connect-datagen connector 
	@docker build -t cp-server-connect-datagen:$(AGGREGATE_VERSION) --build-arg BASE_PREFIX=$(BASE_PREFIX) --build-arg KAFKA_CONNECT_DATAGEN_VERSION=$(KAFKA_CONNECT_DATAGEN_VERSION) --build-arg CP_VERSION=$(CP_VERSION) --build-arg CONNECT_IMAGE=cp-server-connect -f Dockerfile-confluenthub .

# In the case of Operator based images, there is an additional REV version appended to the end of the CP Version
# which allows Operator images to rev independently

build-cp-server-connect-operator-from-local: package ## Build the Docker image based on cp-server-connect from locally mvn built kafka-connect-datagen package
	@docker build -t cp-server-connect-operator-datagen:$(AGGREGATE_LOCAL_VERSION).$(OPERATOR_VERSION) --build-arg BASE_PREFIX=$(BASE_PREFIX) --build-arg KAFKA_CONNECT_DATAGEN_VERSION=$(KAFKA_CONNECT_DATAGEN_LOCAL_VERSION) --build-arg CP_VERSION=$(CP_VERSION).$(OPERATOR_VERSION) --build-arg CONNECT_IMAGE=cp-server-connect-operator -f Dockerfile-local .

build-cp-server-connect-operator-from-released: ## Build a Docker image using a released version of the kafka-connect-datagen connector 
	@docker build -t cp-server-connect-operator-datagen:$(AGGREGATE_VERSION).$(OPERATOR_VERSION) --build-arg BASE_PREFIX=$(BASE_PREFIX) --build-arg KAFKA_CONNECT_DATAGEN_VERSION=$(KAFKA_CONNECT_DATAGEN_VERSION) --build-arg CP_VERSION=$(CP_VERSION).$(OPERATOR_VERSION) --build-arg CONNECT_IMAGE=cp-server-connect-operator -f Dockerfile-confluenthub .

push-from-local:
	@make --no-print-directory build-docker-from-local
	@docker tag kafka-connect-datagen:$(AGGREGATE_LOCAL_VERSION) $(PUSH_PREFIX)/kafka-connect-datagen:$(AGGREGATE_LOCAL_VERSION)
	@docker push $(PUSH_PREFIX)/kafka-connect-datagen:$(AGGREGATE_LOCAL_VERSION)

push-from-released:
	@make --no-print-directory build-docker-from-released
	@docker tag kafka-connect-datagen:$(AGGREGATE_VERSION) $(PUSH_PREFIX)/kafka-connect-datagen:$(AGGREGATE_VERSION)
	@docker push $(PUSH_PREFIX)/kafka-connect-datagen:$(AGGREGATE_VERSION)

push-cp-server-connect-from-local:
	@make --no-print-directory build-cp-server-connect-from-local
	@docker tag cp-server-connect-datagen:$(AGGREGATE_LOCAL_VERSION) $(PUSH_PREFIX)/cp-server-connect-datagen:$(AGGREGATE_LOCAL_VERSION)
	@docker push $(PUSH_PREFIX)/cp-server-connect-datagen:$(AGGREGATE_LOCAL_VERSION)

push-cp-server-connect-from-released:
	@make --no-print-directory build-cp-server-connect-from-released
	@docker tag cp-server-connect-datagen:$(AGGREGATE_VERSION) $(PUSH_PREFIX)/cp-server-connect-datagen:$(AGGREGATE_VERSION)
	@docker push $(PUSH_PREFIX)/cp-server-connect-datagen:$(AGGREGATE_VERSION)

push-cp-server-connect-operator-from-local:
	@make --no-print-directory build-cp-server-connect-operator-from-local
	@docker tag cp-server-connect-operator-datagen:$(AGGREGATE_LOCAL_VERSION).$(OPERATOR_VERSION) $(PUSH_PREFIX)/cp-server-connect-operator-datagen:$(AGGREGATE_LOCAL_VERSION).$(OPERATOR_VERSION)
	@docker push $(PUSH_PREFIX)/cp-server-connect-operator-datagen:$(AGGREGATE_LOCAL_VERSION).$(OPERATOR_VERSION)

push-cp-server-connect-operator-from-released:
	@make --no-print-directory build-cp-server-connect-operator-from-released
	@docker tag cp-server-connect-operator-datagen:$(AGGREGATE_VERSION).$(OPERATOR_VERSION) $(PUSH_PREFIX)/cp-server-connect-operator-datagen:$(AGGREGATE_VERSION).$(OPERATOR_VERSION)
	@docker push $(PUSH_PREFIX)/cp-server-connect-operator-datagen:$(AGGREGATE_VERSION).$(OPERATOR_VERSION)

