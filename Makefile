# Top-level Makefile for java-review

JAVAC ?= javac

.PHONY: all beginner intermediate advanced analyzer clean

all: beginner intermediate advanced analyzer

beginner:
	$(MAKE) -C beginner

intermediate:
	$(MAKE) -C intermediate

advanced:
	$(MAKE) -C advanced

analyzer:
	cd analyzer && mvn -q -DskipTests package

clean:
	-$(MAKE) -C beginner clean
	-$(MAKE) -C intermediate clean
	-$(MAKE) -C advanced clean
	-cd analyzer && mvn -q clean || true
