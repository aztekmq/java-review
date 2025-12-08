# Complete Prometheus Environment Setup (Verbose, Standards-Driven)

This guide describes how to provision a **full Prometheus stack for JVM health monitoring** with verbose logging enabled end to end. All commands, scripts, and configuration snippets follow clear naming, inline comments, and defensible defaults in line with international programming standards.

## 1. Target Architecture

- **Prometheus server** scraping JVM metrics via the JMX exporter and platform telemetry via `node_exporter`.
- **Pushgateway (optional)** for episodic or batch jobs.
- **Grafana** for dashboards and on-demand health reports.
- **Alertmanager (optional)** for paging and ticketing.
- **Verbosity**: every service runs with `--log.level=debug` (or the vendor-equivalent) to simplify diagnostics during onboarding and incident response.

## 2. Prerequisites

- Linux host (systemd-based) with outbound internet access for binary downloads.
- `curl`, `tar`, and `openssl` available in `PATH`.
- Dedicated service account for observability processes, e.g., `observability`.
- Ports open locally: `9090` (Prometheus), `9093` (Alertmanager), `9091` (Pushgateway), `3000` (Grafana), and exporter ports such as `9100` (node_exporter) and `9404` (JMX exporter).

## 3. Directory Layout

Create predictable, standards-aligned directories with explicit ownership and permissions:

```bash
sudo useradd --system --no-create-home --shell /usr/sbin/nologin observability
sudo mkdir -p /etc/prometheus /var/lib/prometheus /opt/jmx_exporter /opt/node_exporter /opt/pushgateway /var/log/prometheus
sudo chown -R observability:observability /etc/prometheus /var/lib/prometheus /opt/jmx_exporter /opt/node_exporter /opt/pushgateway /var/log/prometheus
```

## 4. Install Prometheus (verbose configuration)

```bash
cd /tmp
curl -LO https://github.com/prometheus/prometheus/releases/latest/download/prometheus-$(uname -m).linux-amd64.tar.gz
sudo tar -xzf prometheus-*.tar.gz -C /opt
sudo mv /opt/prometheus-*.linux-amd64 /opt/prometheus
sudo chown -R observability:observability /opt/prometheus
```

Create `/etc/prometheus/prometheus.yml` with explicit scrape jobs and verbose comments:

```yaml
# /etc/prometheus/prometheus.yml
# Verbose logging is configured via the service unit (see below).
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    environment: production
    platform: jvm-health-lab

scrape_configs:
  - job_name: prometheus
    scrape_interval: 15s
    static_configs:
      - targets: ["localhost:9090"]

  - job_name: node_exporter
    metrics_path: /metrics
    static_configs:
      - targets: ["localhost:9100"]
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
        regex: "(.*):.*"
        replacement: "$1"

  - job_name: jvm_applications
    metrics_path: /metrics
    scrape_interval: 15s
    static_configs:
      # Replace with service-specific targets or service discovery.
      - targets:
          - "app-server-01:9404"
          - "app-server-02:9404"
        labels:
          service: billing-service
          env: prod

  - job_name: pushgateway
    honor_labels: true
    static_configs:
      - targets: ["localhost:9091"]
```

Create the Prometheus systemd unit with verbose logging enabled:

```ini
# /etc/systemd/system/prometheus.service
[Unit]
Description=Prometheus Time Series Database (Verbose)
Wants=network-online.target
After=network-online.target

[Service]
User=observability
Group=observability
Type=simple
ExecStart=/opt/prometheus/prometheus \
  --config.file=/etc/prometheus/prometheus.yml \
  --storage.tsdb.path=/var/lib/prometheus \
  --web.listen-address=0.0.0.0:9090 \
  --web.enable-lifecycle \
  --log.level=debug
Restart=on-failure
RestartSec=5s
StandardOutput=append:/var/log/prometheus/prometheus.log
StandardError=inherit

[Install]
WantedBy=multi-user.target
```

Enable and start Prometheus with diagnostics:

```bash
sudo systemctl daemon-reload
sudo systemctl enable prometheus
sudo systemctl start prometheus
sudo journalctl -u prometheus -n 40 -f
```

## 5. Install node_exporter (host metrics)

```bash
cd /tmp
curl -LO https://github.com/prometheus/node_exporter/releases/latest/download/node_exporter-$(uname -m).linux-amd64.tar.gz
sudo tar -xzf node_exporter-*.tar.gz -C /opt
sudo mv /opt/node_exporter-*.linux-amd64 /opt/node_exporter
sudo chown -R observability:observability /opt/node_exporter
```

Systemd unit with verbose flags:

```ini
# /etc/systemd/system/node_exporter.service
[Unit]
Description=Prometheus Node Exporter (Verbose)
Wants=network-online.target
After=network-online.target

[Service]
User=observability
Group=observability
Type=simple
ExecStart=/opt/node_exporter/node_exporter \
  --web.listen-address=:9100 \
  --web.disable-exporter-metrics=false \
  --log.level=debug
Restart=on-failure
RestartSec=5s
StandardOutput=append:/var/log/prometheus/node_exporter.log
StandardError=inherit

[Install]
WantedBy=multi-user.target
```

Enable and verify:

```bash
sudo systemctl daemon-reload
sudo systemctl enable node_exporter
sudo systemctl start node_exporter
curl -v http://localhost:9100/metrics | head -n 20
```

## 6. Install JMX exporter (Java agent)

```bash
cd /tmp
curl -LO https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/1.0.0/jmx_prometheus_javaagent-1.0.0.jar
sudo mv jmx_prometheus_javaagent-1.0.0.jar /opt/jmx_exporter/jmx_prometheus_javaagent.jar
sudo chown observability:observability /opt/jmx_exporter/jmx_prometheus_javaagent.jar
```

Create `/opt/jmx_exporter/jvm.yml` with explicit MBean rules and logging notes:

```yaml
startDelaySeconds: 0
ssl: false
whitelistObjectNames: ["java.lang:type=*", "java.nio:type=BufferPool,*", "java.util.logging:type=Logging"]
rules:
  - pattern: "java.lang:type=MemoryPool,name=(.*)"
    name: jvm_memory_pool_bytes_used
    type: GAUGE
    labels:
      pool: "$1"
    help: "Bytes used for the given JVM memory pool."
  - pattern: "java.lang:type=GarbageCollector,name=(.*)"
    name: jvm_gc_collection_seconds_total
    type: COUNTER
    labels:
      collector: "$1"
    help: "Total GC collection time per collector in seconds."
  - pattern: "java.lang:type=Threading"
    name: jvm_threads_live
    attr: ThreadCount
    type: GAUGE
    help: "Current live thread count."
```

Attach the agent to JVM services with verbose startup banners:

```bash
JAVA_TOOL_OPTIONS="-javaagent:/opt/jmx_exporter/jmx_prometheus_javaagent.jar=9404:/opt/jmx_exporter/jvm.yml"
export JAVA_TOOL_OPTIONS
# Add to your service unit or startup script; retain -Xlog:gc* and JFR flags for richer diagnostics.
```

Validate from the Prometheus host:

```bash
curl -v http://app-server-01:9404/metrics | head -n 20
```

## 7. Install Pushgateway (optional)

```bash
cd /tmp
curl -LO https://github.com/prometheus/pushgateway/releases/latest/download/pushgateway-$(uname -m).linux-amd64.tar.gz
sudo tar -xzf pushgateway-*.tar.gz -C /opt
sudo mv /opt/pushgateway-*.linux-amd64 /opt/pushgateway
sudo chown -R observability:observability /opt/pushgateway
```

Systemd unit with debug logging:

```ini
# /etc/systemd/system/pushgateway.service
[Unit]
Description=Prometheus Pushgateway (Verbose)
Wants=network-online.target
After=network-online.target

[Service]
User=observability
Group=observability
Type=simple
ExecStart=/opt/pushgateway/pushgateway \
  --web.listen-address=0.0.0.0:9091 \
  --log.level=debug
Restart=on-failure
RestartSec=5s
StandardOutput=append:/var/log/prometheus/pushgateway.log
StandardError=inherit

[Install]
WantedBy=multi-user.target
```

## 8. Install Grafana

```bash
sudo apt-get update && sudo apt-get install -y adduser libfontconfig1 musl
curl -LO https://dl.grafana.com/enterprise/release/grafana-enterprise-latest.linux-amd64.tar.gz
sudo tar -xzf grafana-enterprise-*.tar.gz -C /opt
sudo mv /opt/grafana-* /opt/grafana
sudo chown -R observability:observability /opt/grafana
```

Systemd unit with detailed logging:

```ini
# /etc/systemd/system/grafana.service
[Unit]
Description=Grafana Server (Verbose)
Wants=network-online.target
After=network-online.target

[Service]
User=observability
Group=observability
Type=simple
ExecStart=/opt/grafana/bin/grafana-server \
  --homepath=/opt/grafana \
  --config=/opt/grafana/conf/defaults.ini \
  --packaging=standalone \
  --pidfile=/var/run/grafana.pid \
  --log-mode=console \
  --log-level=debug
Restart=on-failure
RestartSec=5s
StandardOutput=append:/var/log/prometheus/grafana.log
StandardError=inherit

[Install]
WantedBy=multi-user.target
```

After starting Grafana, add Prometheus as a data source (`http://localhost:9090`) and import JVM dashboards (e.g., Micrometer JVM dashboard ID `4701`).

## 9. Install Alertmanager (optional)

```bash
cd /tmp
curl -LO https://github.com/prometheus/alertmanager/releases/latest/download/alertmanager-$(uname -m).linux-amd64.tar.gz
sudo tar -xzf alertmanager-*.tar.gz -C /opt
sudo mv /opt/alertmanager-*.linux-amd64 /opt/alertmanager
sudo chown -R observability:observability /opt/alertmanager
```

Minimal configuration with templated routing and verbose logs:

```yaml
# /etc/prometheus/alertmanager.yml
route:
  receiver: default

receivers:
  - name: default
    webhook_configs:
      - url: http://pager-endpoint.example.com/notify
```

Systemd unit:

```ini
# /etc/systemd/system/alertmanager.service
[Unit]
Description=Prometheus Alertmanager (Verbose)
Wants=network-online.target
After=network-online.target

[Service]
User=observability
Group=observability
Type=simple
ExecStart=/opt/alertmanager/alertmanager \
  --config.file=/etc/prometheus/alertmanager.yml \
  --storage.path=/var/lib/prometheus/alertmanager \
  --log.level=debug
Restart=on-failure
RestartSec=5s
StandardOutput=append:/var/log/prometheus/alertmanager.log
StandardError=inherit

[Install]
WantedBy=multi-user.target
```

Update `prometheus.yml` to send alerts:

```yaml
alerting:
  alertmanagers:
    - static_configs:
        - targets: ["localhost:9093"]
```

## 10. Health Checks and Validation

- `curl -v http://localhost:9090/-/ready` (Prometheus readiness)
- `curl -v http://localhost:9090/api/v1/targets | jq '.data.activeTargets[].health'` (target health)
- `curl -v http://localhost:9100/metrics | head -n 5` (node_exporter)
- `curl -v http://app-server-01:9404/metrics | head -n 5` (JMX exporter)
- `curl -v http://localhost:9091/metrics | head -n 5` (Pushgateway)
- `journalctl -u prometheus -u node_exporter -u pushgateway -u grafana -u alertmanager -n 20` (verbose log tails)

## 11. Security and Hardening

- Restrict inbound ports with a host firewall; expose only Grafana/Prometheus through TLS-terminating proxies.
- Pin binary versions in automation and verify checksums when available.
- Configure RBAC/SSO in Grafana; use scoped API keys for automation.
- Set `--web.config.file` for Prometheus if TLS/basic auth is required.

## 12. Maintenance Playbook

- **Config changes**: use `curl -X POST http://localhost:9090/-/reload` after updating `/etc/prometheus/prometheus.yml`; keep changes in version control.
- **Log retention**: rotate files in `/var/log/prometheus/*.log` via `logrotate` to keep verbose logs manageable.
- **Storage**: size `/var/lib/prometheus` based on `--storage.tsdb.retention.time` (default 15d); monitor `tsdb_wal_corruptions_total`.
- **Backups**: snapshot `/etc/prometheus`, `/var/lib/prometheus`, and exporter configs regularly.
- **Upgrades**: perform blue/green or canary restarts; confirm `/api/v1/status/buildinfo` before/after.

With these steps, you can stand up a **fully instrumented Prometheus environment** tailored for JVM health analysis, with verbose logging available at every layer to streamline troubleshooting and uphold international programming standards for documentation and operations.
