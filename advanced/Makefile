# Makefile for advanced labs

JAVAC ?= javac

SRCS = \
A1_low_latency_gc/LowLatencyApp.java \
A2_jfr_profiling/MyServiceAppJfr.java \
A3_container_aware_jvm/MyContainerApp.java

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
	rm -f A3_container_aware_jvm/*.class
	rm -f A3_container_aware_jvm/*.jar
