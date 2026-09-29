LEADING_CORPUS = ../lettermark/cases.yaml
TEST_RESOURCES_DIR = src/test/resources
UNICODE_VERSION = 16.0.0

.DEFAULT_GOAL := build

.PHONY: install-tools comments lint lint-fix format test-build test docs build clean install sync-corpus unicode-sync publish publish-local publish-check

install-tools:
	python3 -m pip install --quiet --upgrade git+https://github.com/botforge-pro/commentcensor.git

comments:
	commentcensor .

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
	./gradlew assemble

clean:
	./gradlew clean

install:
	$(MAKE) install-tools
	./gradlew --version

publish:
	@test -n "$(CI)" || { echo "publish runs in the release workflow, not locally" >&2; exit 1; }
	./gradlew publishAndReleaseToMavenCentral

publish-local:
	./gradlew publishToMavenLocal -PunsignedLocalPublish

publish-check:
	./gradlew publishToMavenLocal

sync-corpus:
	cp $(LEADING_CORPUS) $(TEST_RESOURCES_DIR)/cases.yaml

unicode-sync:
	python3 tools/generate-graphemes.py
	curl -sSf -o $(TEST_RESOURCES_DIR)/GraphemeBreakTest.txt \
		https://www.unicode.org/Public/$(UNICODE_VERSION)/ucd/auxiliary/GraphemeBreakTest.txt
