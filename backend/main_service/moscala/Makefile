# Load environment variables from .env
ifneq (,$(wildcard .env))
  include .env
  export
endif

DB_DEFAULT_URL ?= $(DB_DEFAULT_URL)
DB_DEFAULT_USER ?= $(DB_DEFAULT_USER)
DB_DEFAULT_PASSWORD ?= $(DB_DEFAULT_PASSWORD)

KAFKA_BOOTSTRAP_SERVER ?= $(KAFKA_BOOTSTRAP_SERVER)

SMTP_HOST ?= $(SMTP_HOST)
SMTP_PORT ?= $(SMTP_PORT)
SMTP_USERNAME ?= $(SMTP_USERNAME)
SMTP_PASSWORD ?= $(SMTP_PASSWORD)

SBT := DB_DEFAULT_URL=$(DB_DEFAULT_URL) DB_DEFAULT_USER=$(DB_DEFAULT_USER) DB_DEFAULT_PASSWORD=$(DB_DEFAULT_PASSWORD) sbt

print-db:
	@echo "DB URL: $(DB_DEFAULT_URL)"
	@echo "DB USER: $(DB_DEFAULT_USER)"
	@echo "DB PASSWORD: $(DB_DEFAULT_PASSWORD)"
	@echo "KAFKA SERVER: $(KAFKA_BOOTSTRAP_SERVER)"

.PHONY: migrate run test

migrate:
	$(SBT) flyway/flywayMigrate # flyway configuration is in another subproject to avoid conflict between migration and compilation

run: migrate
	JAVA_OPTS="--add-opens=java.base/java.lang=ALL-UNNAMED" $(SBT) run -deprecation

test: migrate
	$(SBT) -v test

assemble:
	$(SBT) assembly
