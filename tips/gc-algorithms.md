The modern versions of Oracle Java (JDK 11+) focus on two main families of Garbage Collectors: **G1GC** (the default) and the ultra-low-latency collectors like **ZGC**.

Here is a breakdown of the top collectors you should know for your interview:

---

## 1. Garbage-First Collector (G1GC) 🥇 (The Default)
**Key Flag:** `-XX:+UseG1GC` (This is the **default** from Java 9 onwards.)

| Feature | Description |
| :--- | :--- |
| **Goal** | Balance **high throughput** with **predictable pause times**. |
| **Mechanism** | Divides the heap into a large number of fixed-size **regions**. G1 prioritizes collecting regions with the *most garbage* first ("Garbage-First").  |
| **Pause Reduction** | Allows the user to specify a soft pause time goal (`-XX:MaxGCPauseMillis=<N>`) and uses concurrent marking to reduce the *Stop-The-World (STW)* time. |
| **Compaction** | It is a **compacting** collector, which helps prevent heap fragmentation, unlike the older CMS collector. |
| **Use Case** | The **general-purpose recommendation** for modern server applications, especially those with large heaps (6GB+). |

---

## 2. Z Garbage Collector (ZGC) ⚡ (Ultra-Low Latency)
**Key Flag:** `-XX:+UseZGC` (Production-ready since JDK 15)

| Feature | Description |
| :--- | :--- |
| **Goal** | Achieve **ultra-low, consistent pause times** (typically <10ms, often <1ms), regardless of heap size. |
| **Mechanism** | Performs nearly all work, including **compaction**, **concurrently** with application threads. It uses advanced techniques like **Colored Pointers** and **Load Barriers**. |
| **Scalability** | Designed to scale to **massive heaps** (multi-terabyte). Pause times do *not* increase with heap size. |
| **Trade-offs** | It uses more CPU resources due to its high level of concurrency and has a higher base memory footprint. |
| **Use Case** | **Latency-critical systems** like High-Frequency Trading (HFT), real-time data processing, and large in-memory databases where consistent sub-millisecond responsiveness is paramount. |

---

## 3. Shenandoah GC 💨 (Low Latency, OpenJDK/Red Hat)
**Key Flag:** `-XX:+UseShenandoahGC` (Integrated into OpenJDK, similar goals to ZGC)

* While officially part of the OpenJDK project (which Oracle builds upon), Shenandoah was initially developed by Red Hat. It's often discussed alongside ZGC.
* **Goal:** Achieves similar **ultra-low pause times** to ZGC by performing **concurrent compaction** using *Brooks Pointers*.
* **Key Difference from ZGC:** It has a different implementation approach for concurrent compaction.

### Summary for Your Interview:

1.  **G1GC:** **The Default**. Good balance of throughput and predictable latency for most enterprise applications.
2.  **ZGC:** For **Ultra-Low Latency**. Consistently sub-10ms pauses, even on massive heaps. The best answer for "latency-critical" applications.
