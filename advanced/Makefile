# Makefile for advanced labs

JAVAC ?= javac

SRCS = \
A1_low_latency_gc/LowLatencyApp.java \
<<<<<<< HEAD
A2_jfr_profiling/MyServiceAppJfr.java
=======
A2_jfr_profiling/MyServiceAppJfr.java \
A3_async_profiler/AsyncProfilerLab.java
>>>>>>> ffa9d3032c6e0821101493eb0ada21a66d2bb267

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
<<<<<<< HEAD
=======
	rm -f A3_async_profiler/*.class
	rm -f A3_async_profiler/*.log
	rm -f A3_async_profiler/*.jfr
	rm -f A3_async_profiler/*.svg
>>>>>>> ffa9d3032c6e0821101493eb0ada21a66d2bb267
