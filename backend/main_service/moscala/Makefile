# Load environment variables from .env
ifneq (,$(wildcard .env))
  include .env
  export
endif

DB_URL ?= $(DB_URL)
DB_USER ?= $(DB_USER)
DB_PASSWORD ?= $(DB_PASSWORD)

APP_SECRET_KEY ?= $(APP_SECRET_KEY)
JWT_SECRET ?= $(JWT_SECRET)

KAFKA_BOOTSTRAP_SERVER ?= $(KAFKA_BOOTSTRAP_SERVER)

REDIS_HOST ?= $(REDIS_HOST)
REDIS_PORT ?= $(REDIS_PORT)

SMTP_HOST ?= $(SMTP_HOST)
SMTP_PORT ?= $(SMTP_PORT)
SMTP_USER ?= $(SMTP_USER)
SMTP_PASSWORD ?= $(SMTP_PASSWORD)

SBT := DB_URL=$(DB_URL) DB_USER=$(DB_USER) DB_PASSWORD=$(DB_PASSWORD) sbt

.PHONY: migrate run test

run:
	JAVA_OPTS="--add-opens=java.base/java.lang=ALL-UNNAMED" $(SBT) run -deprecation

test:
	$(SBT) -v test

assemble:
	$(SBT) assembly
