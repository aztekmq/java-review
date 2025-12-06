# Makefile for beginner labs
# Explicitly compile with a fixed Java release to avoid runtime/class
# version mismatches when running the exercises on older JDKs.

JAVAC ?= javac
JAVA_RELEASE ?= 20

SRCS = \
	B1_gc_basics/GcBasics.java \
	B2_heap_sizing/HeapStress.java \
	B3_thread_states/ThreadStatesDemo.java

CLASSES = $(SRCS:.java=.class)

.PHONY: all clean

all: $(CLASSES)

%.class: %.java
	$(JAVAC) --release $(JAVA_RELEASE) $<

clean:
	rm -f B1_gc_basics/*.class
	rm -f B2_heap_sizing/*.class
	rm -f B3_thread_states/*.class
	rm -f B2_heap_sizing/*.hprof
