# Prometheus-Centric JVM Health Platform (One-Command / One-Click)

This guide pivots the JVM Health Analyzer story from a VisualVM plugin concept to a **Prometheus-first health platform** that junior engineers can drive with a single CLI command or Grafana button. All collection steps emphasize **verbose logging for debuggability** and adhere to international programming standards for naming, comments, and operational clarity.

## 0. High-Level Shape

**Stack overview**

- **Data collectors**
  - JVM ➜ Prometheus via **JMX exporter Java agent** or **Micrometer/Actuator**.
  - OS ➜ **node_exporter** (optionally cAdvisor for container scopes).
  - Optional **GC log/JFR sidecar** that parses evidence and publishes derived metrics into Prometheus (via Pushgateway or remote_write).
- **Analyzer service**
  - Talks to the Prometheus HTTP API with **PromQL** and traces each request with verbose logs.
  - Applies **YAML rule definitions** to emit findings and health scores.
- **Report generator**
  - Renders HTML/PDF with charts + explanations, keeping transparent logging for each render step.
- **LLM explainer (optional)**
  - Converts findings into narrative guidance and recommendations.
- **Frontends**
  - **CLI** (`jvm-health-report ...`).
  - **Grafana button/panel** (“Generate JVM Health Report”).

## 1. Data Collection – Prometheus Style

### 1.1 JVM metrics

Use either **JMX exporter** or **Micrometer/Actuator** with a Prometheus endpoint. Keep startup scripts verbose so operators can see the agent wiring.

```bash
# Java agent example (verbose paths and port)
JAVA_TOOL_OPTIONS="-javaagent:/opt/jmx_exporter/jmx_prometheus_javaagent.jar=9404:/opt/jmx_exporter/jvm.yml"
export JAVA_TOOL_OPTIONS
```

Key JVM metrics to scrape:

- **Memory / heap**: `jvm_memory_used_bytes{area="heap"}`, `jvm_memory_max_bytes{area="heap"}`, pool breakdown (`G1_Old_Gen`, `PS_Old_Gen`, etc.).
- **GC**: `jvm_gc_pause_seconds_count`, `jvm_gc_pause_seconds_sum`, `jvm_gc_pause_seconds_max` (if exposed), collector-specific `jvm_gc_collections_seconds_*`.
- **Threads**: `jvm_threads_live`, `jvm_threads_daemon`, `jvm_threads_peak`.
- **Class loading / metaspace**: `jvm_classes_loaded_classes`, `jvm_memory_used_bytes{area="nonheap", ...}`.

### 1.2 OS & container metrics

Collect with **node_exporter** (and **cAdvisor** for containers): `node_cpu_seconds_total`, `node_memory_Active_bytes`, `node_load1/5`, `container_cpu_usage_seconds_total`, `container_memory_working_set_bytes`.

### 1.3 Optional: GC log / JFR sidecar

Run a sidecar or cron job that parses GC logs/JFR and publishes derived metrics—e.g., `jvm_gc_full_gc_count`, `jvm_gc_promotion_failed_total`, `jvm_alloc_rate_bytes_per_second`, `jvm_gc_humongous_alloc_total`. Emit verbose parser logs to simplify debugging during onboarding.

## 2. Analyzer Engine (Rules + PromQL)

The analyzer is a small service (Go/Java/Python) that:

1. Accepts `service`, `env`, and a time window (`--from 24h`).
2. Executes **PromQL** to compute KPIs, logging each query and response size for traceability.
3. Applies **YAML rules** to classify findings.

Example KPIs (24h window):

```promql
# GC overhead % over last 24h
100 * sum_over_time(jvm_gc_pause_seconds_sum{service="billing-service"}[24h])
  /
sum_over_time(process_uptime_seconds{service="billing-service"}[24h])

# P95 GC pause (1h rate window)
histogram_quantile(
  0.95,
  sum by (le) (rate(jvm_gc_pause_seconds_bucket{service="billing-service"}[1h]))
)

# Max heap usage %
max_over_time(
  100 * jvm_memory_used_bytes{area="heap", service="billing-service"}
  /
  jvm_memory_max_bytes{area="heap", service="billing-service"}
  [24h]
)

# CPU saturation (avg CPU usage / cores)
avg_over_time(
  100 * rate(node_cpu_seconds_total{mode!="idle", instance=~"billing.*"}[5m])
)[24h:]
```

### 2.1 YAML rule format

```yaml
rules:
  - id: GC_OVERHEAD_HIGH
    category: gc
    description: "GC overhead is too high"
    metric: gc_overhead_percent
    severity:
      - when: "value > 15"
        level: critical
        message: "GC overhead > 15% of runtime. Application may be GC-bound."
      - when: "value > 8"
        level: warning
        message: "GC overhead > 8%. Watch for increased pause times under load."

  - id: HEAP_OVERSIZED
    category: memory
    description: "Heap likely oversized"
    metric: max_heap_usage_percent
    severity:
      - when: "value < 35"
        level: info
        message: "Heap usage never exceeded 35%. You might safely reduce -Xmx."

  - id: CPU_SATURATION
    category: cpu
    metric: avg_cpu_usage_percent
    severity:
      - when: "value > 85"
        level: critical
        message: "Average CPU > 85%. Consider scaling up or out."
      - when: "value > 70"
        level: warning
        message: "Average CPU > 70%. Monitor closely; nearing saturation."
```

Analyzer output is a structured JSON payload with health scores and triggered findings, suitable for CLI, Grafana, or LLM consumption.

## 3. Report Generator (HTML/PDF)

Inputs: analyzer JSON (scores, findings, KPIs), raw PromQL time series for charts, and metadata (service, env, JVM version, GC type, instance list).

Outputs: HTML/PDF “JVM Health Report” with:

1. **Cover/summary** – Service, env, window, and area scores (✅/🟡/🔴).
2. **GC section** – GC overhead trends, P95 pause chart, GC algorithm summary, triggered rules.
3. **Memory section** – Heap used vs max, old-gen occupancy, oversizing/leak/humongous notes.
4. **CPU & Threads** – CPU usage vs cores, live threads over time, saturation or thread explosion callouts.
5. **Action items** – 5–10 prioritized, safe recommendations.

Each render step should log verbosely (template chosen, chart data ranges, PDF conversion command) to aid incident-debug workflows.

## 4. LLM Explainer (Optional)

Feed a condensed analyzer JSON into a local or remote LLM with this pattern:

> “You are a senior JVM performance engineer. Given the metrics and findings below, write:
> 1. a non-technical executive summary (3–5 sentences),
> 2. a technical summary for dev/ops,
> 3. a bulleted list of safe, conservative tuning recommendations that a junior engineer could implement with supervision.”

The generated text becomes additional sections in the PDF. Persist prompt/response logs (with PII-safety controls) for auditability.

## 5. Frontends

### 5.1 CLI

```bash
jvm-health-report \
  --service billing-service \
  --env prod \
  --from 24h \
  --output report-billing-prod-$(date +%F).pdf
```

Flow: query Prometheus ➜ apply rules ➜ optional LLM ➜ render HTML ➜ convert to PDF. Print verbose status: PromQL timing, rule file path, report location.

### 5.2 Grafana button/panel

Add a Grafana panel titled **“JVM Health Report”** with a **“Generate report for last 24h”** button. The button calls the backend over HTTP; the backend generates and returns a PDF download link (or pushes to ticket/S3). Log each request, including user identity and time window, to simplify troubleshooting.

### 5.3 CI / scheduled job

```bash
jvm-health-report --all-services --env prod --from 24h --upload-to s3://jvm-reports/
```

Schedule nightly runs to build a historical archive of JVM health reports. Preserve verbose job logs so on-call engineers can retrace any collection or rendering step.

## 6. What This Enables for an Entry-Level Programmer

With labels set in Prometheus (`service`, `env`), a novice can run one command or click one Grafana button, then attach the PDF to a ticket:

> “Attached is the automated JVM Health Report for `billing-service` in `prod` for the last 24h. The tool flagged high CPU saturation and moderate GC overhead; heap looks oversized. See ‘Action Items’ for next steps.”

The intelligence stays in the **YAML rules** and **LLM prompt**—tuned by senior engineers—while verbose logging and clear scripts make the workflow transparent for learners.
