# Makefile for advanced labs

JAVAC ?= javac

SRCS = \
A1_low_latency_gc/LowLatencyApp.java \
A2_jfr_profiling/MyServiceAppJfr.java

CLASSES = $(SRCS:.java=.class)

.PHONY: all clean

all: $(CLASSES)
	@echo "===> Advanced labs compilation started"

%.class: %.java
	@echo "Compiling $<"
	$(JAVAC) $<

clean:
	@echo "===> Cleaning advanced lab artifacts"
	rm -f A1_low_latency_gc/*.class
	rm -f A1_low_latency_gc/*.log
	rm -f A2_jfr_profiling/*.class
	rm -f A2_jfr_profiling/*.jfr
