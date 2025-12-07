# ThreadStatesDemo artifact creation guide

This lab starts `ThreadStatesDemo` with Java Flight Recorder (JFR) and GC
logging enabled so you can collect artifacts while the threads sit in their
illustrative states.

## What the launch command does

The suggested launch command is:

```
java -Xms256m -Xmx256m \
  -XX:StartFlightRecording=filename=beginner-b3.jfr,dumponexit=true,settings=profile \
  -Xlog:gc*:file=beginner-b3-gc.log:uptime,time,level,tags \
  -cp beginner/B3_thread_states ThreadStatesDemo
```

* `-XX:StartFlightRecording=...` starts a JFR session as soon as the JVM
  begins. Setting `filename=beginner-b3.jfr` writes the recording directly to
  that file. The `dumponexit=true` flag guarantees the file is flushed to disk
  when the process terminates, even if you stop it with `Ctrl+C` or `jcmd
  ... VM.stop`.
* `-Xlog:gc*:file=beginner-b3-gc.log:uptime,time,level,tags` enables verbose GC
  logging and streams it to `beginner-b3-gc.log` with timestamps so you can
  correlate events with the JFR capture.
* The `ThreadStatesDemo` code creates threads that stay in RUNNABLE,
  TIMED_WAITING, and BLOCKED states. The main thread sleeps for two minutes to
  keep the process alive while you inspect it.

## Where the artifacts appear

By default, both artifacts are written in the directory where you ran the
command:

* **JFR recording**: `beginner-b3.jfr` – finalized automatically on JVM exit
  because of `dumponexit=true`.
* **GC log**: `beginner-b3-gc.log` – streamed as the JVM runs, then closed on
  exit.

You can ingest `beginner-b3.jfr` directly into JDK Mission Control or any JFR
viewer. The GC log can be opened in tools that accept Unified Logging output
(such as GCViewer or GCToolkit) or inspected with standard text utilities.

## Tips for capturing a clean recording

1. Start the program with the command above in the repository root so the
   artifact paths line up with the lab instructions.
2. Let it run long enough for the threads to settle (the initial console output
   shows when the lockers contend for the monitor). The program is meant to sit
   idle in those states until you stop it.
3. When you are done, terminate the JVM (e.g., `Ctrl+C`). JFR will dump
   `beginner-b3.jfr`, and the GC log will be flushed. No extra commands are
   required to create the files.
