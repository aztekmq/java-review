# Makefile for intermediate labs

JAVAC ?= javac

SRCS = \
I1_gc_tuning_g1/MyServiceApp.java \
I2_memory_leak_lab/LeakLab.java \
I3_thread_dump_lock_contention/LockContentionLab.java

CLASSES = $(SRCS:.java=.class)

.PHONY: all clean

all: $(CLASSES)

%.class: %.java
$(JAVAC) $<

clean:
rm -f I1_gc_tuning_g1/*.class
rm -f I2_memory_leak_lab/*.class
rm -f I2_memory_leak_lab/*.hprof
rm -f I3_thread_dump_lock_contention/*.class
rm -f I1_gc_tuning_g1/gc.log
