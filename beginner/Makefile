# Makefile for beginner labs

JAVAC ?= javac

SRCS = \
B1_gc_basics/GcBasics.java \
B2_heap_sizing/HeapStress.java \
B3_thread_states/ThreadStatesDemo.java

CLASSES = $(SRCS:.java=.class)

.PHONY: all clean

all: $(CLASSES)

%.class: %.java
$(JAVAC) $<

clean:
rm -f B1_gc_basics/*.class
rm -f B2_heap_sizing/*.class
rm -f B3_thread_states/*.class
rm -f B2_heap_sizing/*.hprof
