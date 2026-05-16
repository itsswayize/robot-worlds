# Makefile for Robot Worlds
# NOTE: Run this Makefile from a POSIX-like shell (bash).
SHELL := /usr/bin/env bash

ROOT := $(CURDIR)
REF_JAR := $(ROOT)/lib/reference-server-0.2.3.jar
REF_JAR2 := $(ROOT)/lib/reference-server-0.1.0.jar
JAR_PATTERN := target/*-jar-with-dependencies.jar

.PHONY: deps package build start-ref stop-ref start-local stop-local \
    iteration2-local-launch iteration2-ref-launch iteration2-ref-look iteration2-local-look \
    ref-all-except-iteration2 local-all-except-iteration2 full-orchestrated

# -----------------------------
# Basic build helpers
# -----------------------------
deps:
	@echo "Resolving dependencies with Maven..."
	@mvn dependency:purge-local-repository -DactTransitively=false
	@mvn -q dependency:resolve

package:
	@echo "Building fat jar (skip tests)"
	@mvn -DskipTests package

build: deps package

# -----------------------------
# Reference server control (v0.2.3)
# -----------------------------
start-ref:
	@if [ ! -f "$(REF_JAR)" ]; then \
		echo "Reference server jar not found at $(REF_JAR)"; \
		exit 1; \
	fi
	@echo "Checking for existing listener on port 5000..."
	@EXIST_PID=$$(ss -ltnp 2>/dev/null | awk '/:5000/ { gsub(/.*pid=/,"",$$NF); gsub(/,.*/,"",$$NF); print $$NF; exit }'); \
	if [ -n "$$EXIST_PID" ]; then \
		echo "Port 5000 already in use by PID $$EXIST_PID; killing it to free port..."; \
		kill -9 $$EXIST_PID 2>/dev/null || true; \
		sleep 1; \
	fi
	@echo "Starting reference server ($(REF_JAR)) with args: '$(REF_ARGS)'; logging to .ref.log"
	@nohup java -jar "$(REF_JAR)" $(REF_ARGS) > .ref.log 2>&1 & echo $$! > .ref.pid
	@echo "Waiting up to 15s for reference server to accept connections on port 5000..."
	@i=0; \
	while ! nc -z localhost 5000 2>/dev/null; do \
		sleep 1; \
		i=$$((i+1)); \
		if [ $$i -ge 15 ]; then break; fi; \
	done; \
	if ! nc -z localhost 5000 2>/dev/null; then \
		echo "Reference server failed to accept connections on port 5000 after 15s"; \
		echo "--- .ref.log (last 200 lines) ---"; tail -n 200 .ref.log; \
		exit 1; \
	fi
	@echo "Reference server PID: $$(cat .ref.pid) (logs: .ref.log)"

# -----------------------------
# Reference server control (v0.1.0)
# -----------------------------
start-ref2:
	@if [ ! -f "$(REF_JAR2)" ]; then \
		echo "Reference server jar not found at $(REF_JAR2)"; \
		exit 1; \
	fi
	@echo "Checking for existing listener on port 5000..."
	@EXIST_PID=$$(ss -ltnp 2>/dev/null | awk '/:5000/ { gsub(/.*pid=/,"",$$NF); gsub(/,.*/,"",$$NF); print $$NF; exit }'); \
	if [ -n "$$EXIST_PID" ]; then \
		echo "Port 5000 already in use by PID $$EXIST_PID; killing it to free port..."; \
		kill -9 $$EXIST_PID 2>/dev/null || true; \
		sleep 1; \
	fi
	@echo "Starting reference server ($(REF_JAR2)) with args: '$(REF_ARGS)'; logging to .ref.log"
	@nohup java -jar "$(REF_JAR2)" $(REF_ARGS) > .ref.log 2>&1 & echo $$! > .ref.pid
	@echo "Waiting up to 15s for reference server to accept connections on port 5000..."
	@i=0; \
	while ! nc -z localhost 5000 2>/dev/null; do \
		sleep 1; \
		i=$$((i+1)); \
		if [ $$i -ge 15 ]; then break; fi; \
	done; \
	if ! nc -z localhost 5000 2>/dev/null; then \
		echo "Reference server failed to accept connections on port 5000 after 15s"; \
		echo "--- .ref.log (last 200 lines) ---"; tail -n 200 .ref.log; \
		exit 1; \
	fi
	@echo "Reference server PID: $$(cat .ref.pid) (logs: .ref.log)"

stop-ref:
	@echo "Stopping reference server (if running)"
	@test -f .ref.pid && kill $$(cat .ref.pid) 2>/dev/null || true
	@rm -f .ref.pid || true
	@echo "Reference log: .ref.log"

# -----------------------------
# Local server control (built jar)
# -----------------------------
start-local:
	@JAR=$$(ls $(JAR_PATTERN) 2>/dev/null | head -n1 || true); \
	if [ -z "$$JAR" ]; then \
		echo "Built jar not found, running make package..."; \
		$(MAKE) package; \
		JAR=$$(ls $(JAR_PATTERN) | head -n1); \
	fi; \
	EXIST_PID=$$(ss -ltnp 2>/dev/null | awk '/:5000/ { gsub(/.*pid=/,"",$$NF); gsub(/,.*/,"",$$NF); print $$NF; exit }'); \
	if [ -n "$$EXIST_PID" ]; then \
		echo "Port 5000 already in use by PID $$EXIST_PID; killing it to free port..."; \
		kill -9 $$EXIST_PID 2>/dev/null || true; \
		sleep 1; \
	fi; \
	if [ -z "$(LOCAL_ARGS)" ]; then ARGS=""; else ARGS="$(LOCAL_ARGS)"; fi; \
	echo "Starting local server ($$JAR) with args: '$$ARGS' (logs: .local.log)"; \
	nohup java -jar "$$JAR" $$ARGS > .local.log 2>&1 & echo $$! > .local.pid; \
	@echo "Waiting up to 15s for local server to accept connections on port 5000..."; \
	i=0; \
	while ! nc -z localhost 5000 2>/dev/null; do \
		sleep 1; \
		i=$$((i+1)); \
		if [ $$i -ge 15 ]; then break; fi; \
	done; \
	if ! nc -z localhost 5000 2>/dev/null; then \
		echo "Local server failed to accept connections on port 5000 after 15s"; \
		echo "--- .local.log (last 200 lines) ---"; tail -n 200 .local.log; \
		exit 1; \
	fi; \
	echo "Local server PID: $$(cat .local.pid) (logs: .local.log)"

stop-local:
	@echo "Stopping local server (if running)"
	@test -f .local.pid && kill $$(cat .local.pid) 2>/dev/null || true
	@rm -f .local.pid || true
	@echo "Local log: .local.log"

# -----------------------------
# Orchestrated test sequences
# -----------------------------

iteration2-local-launch:
	@echo "Starting local server with -p 5000 -s 2 -o 1,1 and running LaunchRobot tests..."
	@$(MAKE) LOCAL_ARGS="-p 5000 -s 2 -o 1,1" start-local
	@echo "Running LaunchRobot tests against local server..."
	@if ! mvn -Dtest=za.co.wethinkcode.robots.acceptance.iteration2.LaunchRobot test; then \
		$(MAKE) stop-local; exit 1; \
	fi
	@$(MAKE) stop-local

iteration2-ref-launch:
	@REF_ARGS="-p 5000 -s 2 -o 1,1" $(MAKE) start-ref
	@echo "Running LaunchRobot tests against reference server..."
	@if ! mvn -Dtest=za.co.wethinkcode.robots.acceptance.iteration2.LaunchRobot test; then \
		$(MAKE) stop-ref; exit 1; \
	fi
	@$(MAKE) stop-ref

iteration2-ref-look:
	@REF_ARGS="-p 5000 -s 2 -o 0,1" $(MAKE) start-ref
	@echo "Running Look test against reference server (obstacle at 0,1)..."
	@if ! mvn -Dtest=za.co.wethinkcode.robots.acceptance.iteration2.Look test; then \
		$(MAKE) stop-ref; exit 1; \
	fi
	@$(MAKE) stop-ref

iteration2-local-look:
	@echo "Starting local server with -p 5000 -s 2 -o 0,1 and running Look test..."
	@$(MAKE) LOCAL_ARGS="-p 5000 -s 2 -o 0,1" start-local
	@echo "Running Look test against local server (obstacle at 0,1)..."
	@if ! mvn -Dtest=za.co.wethinkcode.robots.acceptance.iteration2.Look test; then \
		$(MAKE) stop-local; exit 1; \
	fi
	@$(MAKE) stop-local

# -----------------------------
# Run all tests except iteration2
# -----------------------------

ref-all-except-iteration2:
	@REF_ARGS="-p 5000 -s 1" $(MAKE) start-ref2
	@echo "Running all tests against reference server excluding iteration2 package..."
	@if ! mvn test -DexcludedTests="**/acceptance/iteration2/**"; then \
		$(MAKE) stop-ref; exit 1; \
	fi
	@$(MAKE) stop-ref

local-all-except-iteration2:
	@LOCAL_ARGS="-p 5000 -s 1" $(MAKE) start-local
	@echo "Running all tests against local server excluding iteration2 package..."
	@if ! mvn test -DexcludedTests="za/co/wethinkcode/robots/acceptance/iteration2/**/*Test.java"; then \
		$(MAKE) stop-local; exit 1; \
	fi
	@$(MAKE) stop-local

# -----------------------------
# Full orchestrated flows
# -----------------------------
test: iteration2-local-launch iteration2-ref-launch iteration2-ref-look iteration2-local-look ref-all-except-iteration2 local-all-except-iteration2
	@echo "Full orchestration complete."

# Backwards compat
start-ref-default:
	@REF_ARGS="-p 5000 -s 2 -o none" $(MAKE) start-ref

start-local-default:
	@LOCAL_ARGS="-p 5000 -s 2 -o none" $(MAKE) start-local