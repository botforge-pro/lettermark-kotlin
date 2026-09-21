LEADING_CORPUS = ../lettermark/cases.yaml
TEST_RESOURCES_DIR = src/test/resources
UNICODE_VERSION = 16.0.0
COMMENTCENSOR_VERSION ?= v0.3.3
COMMENTCENSOR_ENV = build/commentcensor
COMMENTCENSOR = $(COMMENTCENSOR_ENV)/bin/commentcensor

.DEFAULT_GOAL := build

.PHONY: install-tools comments lint lint-fix format test-build test docs build clean install sync-corpus unicode-sync

install-tools:
	python3 -m venv $(COMMENTCENSOR_ENV)
	$(COMMENTCENSOR_ENV)/bin/pip install --quiet --upgrade git+https://github.com/botforge-pro/commentcensor.git@$(COMMENTCENSOR_VERSION)

comments:
	$(COMMENTCENSOR) .

lint: comments
	./gradlew ktlintCheck

lint-fix:
	./gradlew ktlintFormat

format: lint-fix

test:
	./gradlew test

test-build:
	./gradlew compileTestKotlin

docs:
	./gradlew dokkaGeneratePublicationHtml

build: lint test-build test docs
	./gradlew build

clean:
	./gradlew clean

install:
	$(MAKE) install-tools
	./gradlew --version

sync-corpus:
	cp $(LEADING_CORPUS) $(TEST_RESOURCES_DIR)/cases.yaml

unicode-sync:
	python3 tools/generate-graphemes.py
	curl -sSf -o $(TEST_RESOURCES_DIR)/GraphemeBreakTest.txt \
		https://www.unicode.org/Public/$(UNICODE_VERSION)/ucd/auxiliary/GraphemeBreakTest.txt
