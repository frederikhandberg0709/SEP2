PROJECT_NAME = chat-app
MAIN_CLASS = via.sep2.App

MAVEN_OPTS ?= -Xmx1024m
MAVEN_PROFILES ?=

COLOR_RESET = \033[0m
COLOR_GREEN = \033[32m
COLOR_YELLOW = \033[33m
COLOR_RED = \033[31m
COLOR_BLUE = \033[34m

.PHONY: run
run:
	@echo "$(COLOR_GREEN)Running $(PROJECT_NAME)...$(COLOR_RESET)"
	@mvn javafx:run

PHONY: clean
clean:
	@echo "$(COLOR_GREEN)Cleaning project...$(COLOR_RESET)"
	@mvn clean

.PHONY: install
install:
	@echo "$(COLOR_GREEN)Installing to local repository...$(COLOR_RESET)"
	@mvn install
