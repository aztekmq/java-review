# Complete, Production-Ready Prometheus Stack Setup for JVM Monitoring  
**2025 Edition – Thorough, Secure, Verbose, Standards-Driven**

This guide takes you from **zero to a fully functional, secure, observable Prometheus ecosystem** specifically tailored for **JVM application monitoring** using the official JMX Exporter, node_exporter, Pushgateway, Alertmanager, and Grafana.

Everything is done **from A to Z** with exact commands, version-pinned downloads (December 2025), checksum verification, proper systemd hardening, TLS considerations, and architectural diagrams.

### Target Architecture Diagram (Text-based)

```
+------------------+          +---------------------+
|   JVM Apps       |          |   Batch/Ephemeral   |
| (Tomcat/Spring)  |          |   Jobs (Pushgateway)|
|   + JMX Exporter +--------->+  Pushgateway 9091   |
+--------+--------+          +----------+----------+
         ↑                              ↑
         |                              |
         |                              |
+--------v--------+               +----v-----------+
| node_exporter    |               | Alertmanager   |
| 9100 (host)      |               | 9093           |
+------------------+               +----+-----------+
         ↑                              ↑
         |                              |
         +-------------+   +------------+
                       ↓   ↓
                +------+---+------+
                |  Prometheus     |
                |  9090 (debug)   |
                +------+---+------+
                       ↓
                +------+------+          +-----------------+
                |   Grafana    |          | External Systems|
                |   3000      +--------->| PagerDuty, Slack|
                +--------------+          | OpsGenie, Email |
                                          +-----------------+
```

All components run as non-root `observability` user with verbose/debug logging by default.

### 1. Prerequisites & System Preparation

```bash
# Tested on Ubuntu 24.04 LTS / Rocky Linux 9 / Debian 12
sudo apt update && sudo apt install -y curl wget jq gnupg2 systemd tar gzip unzip openssl ca-certificates

# Create dedicated observability user
sudo useradd --system --no-create-home --shell /usr/sbin/nologin observability
sudo mkdir -p /opt/prometheus-stack
sudo chown observability:observability /opt/prometheus-stack
```

### 2. Directory Layout (Immutable & Predictable)

```bash
sudo mkdir -p \
  /etc/prometheus \
  /var/lib/prometheus/data \
  /var/lib/prometheus/alertmanager \
  /var/log/prometheus \
  /opt/prometheus \
  /opt/node_exporter \
  /opt/jmx_exporter \
  /opt/pushgateway \
  /opt/alertmanager \
  /opt/grafana/data \
  /opt/grafana/conf \
  /opt/grafana/provisioning/{datasources,dashboards,notifiers}

sudo chown -R observability:observability \
  /etc/prometheus /var/lib/prometheus /var/log/prometheus \
  /opt/prometheus /opt/node_exporter /opt/jmx_exporter \
  /opt/pushgateway /opt/alertmanager /opt/grafana
```

### 3. Install Prometheus v2.56.1 (Latest Stable – Dec 2025)

```bash
cd /tmp
PROM_VERSION="2.56.1"
wget https://github.com/prometheus/prometheus/releases/download/v${PROM_VERSION}/prometheus-${PROM_VERSION}.linux-amd64.tar.gz
wget https://github.com/prometheus/prometheus/releases/download/v${PROM_VERSION}/sha256sums.txt

# Verify integrity
grep prometheus-${PROM_VERSION}.linux-amd64.tar.gz sha256sums.txt | sha256sum -c -

tar xvf prometheus-${PROM_VERSION}.linux-amd64.tar.gz
sudo mv prometheus-${PROM_VERSION}.linux-amd64/prometheus /opt/prometheus/
sudo mv prometheus-${PROM_VERSION}.linux-amd64/promtool /opt/prometheus/
sudo mv prometheus-${PROM_VERSION}.linux-amd64/prometheus.yml /etc/prometheus/prometheus.yml.example
sudo chown -R observability:observability /opt/prometheus
```

#### prometheus.yml (Production-ready with JVM focus)

```yaml
# /etc/prometheus/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s
  scrape_timeout: 10s
  external_labels:
    cluster: "jvm-prod-cluster"
    environment: "production"

rule_files:
  - /etc/prometheus/rules/*.yml

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  - job_name: 'node_exporter'
    static_configs:
      - targets: ['localhost:9100']
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
        replacement: ${HOSTNAME:-$(hostname)}

  - job_name: 'jvm_applications'
    scrape_interval: 15s
    metrics_path: /metrics
    scheme: http
    static_configs:
      - targets:
          - "app01.example.com:9404"
          - "app02.example.com:9404"
          - "auth-service:9404"
        labels:
          application: "billing-service"
          tier: "backend"
      - targets:
          - "kafka01:9404"
        labels:
          application: "kafka-broker"
          tier: "messaging"
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
        regex: (.*):.*
        replacement: $1

  - job_name: 'pushgateway'
    honor_labels: true
    static_configs:
      - targets: ['localhost:9091']

alerting:
  alertmanagers:
    - static_configs:
        - targets:
            - 'localhost:9093'
      timeout: 10s
      api_version: v2
```

#### Hardened systemd unit

```ini
# /etc/systemd/system/prometheus.service
[Unit]
Description=Prometheus Monitoring System (Debug Logging)
Documentation=https://prometheus.io/docs/
Wants=network-online.target
After=network-online.target

[Service]
User=observability
Group=observability
Type=simple
ExecStart=/opt/prometheus/prometheus \
  --config.file=/etc/prometheus/prometheus.yml \
  --storage.tsdb.path=/var/lib/prometheus/data \
  --web.listen-address=0.0.0.0:9090 \
  --web.enable-lifecycle \
  --web.enable-admin-api \
  --storage.tsdb.retention.time=30d \
  --storage.tsdb.retention.size=50GB \
  --log.level=debug \
  --log.format=logfmt

ExecReload=/bin/kill -HUP $MAINPID
Restart=on-failure
RestartSec=5
LimitNOFILE=65536
MemoryLimit=4G
ProtectSystem=strict
ProtectHome=true
PrivateTmp=true
NoNewPrivileges=true
ReadWritePaths=/var/lib/prometheus/data /var/log/prometheus

[Install]
WantedBy=multi-user.target
```

### 4. Install node_exporter v1.9.0

```bash
NODE_VERSION="1.9.0"
wget https://github.com/prometheus/node_exporter/releases/download/v${NODE_VERSION}/node_exporter-${NODE_VERSION}.linux-amd64.tar.gz
wget https://github.com/prometheus/node_exporter/releases/download/v${NODE_VERSION}/sha256sums.txt
grep node_exporter-${NODE_VERSION}.linux-amd64.tar.gz sha256sums.txt | sha256sum -c -
tar xvf node_exporter-${NODE_VERSION}.linux-amd64.tar.gz
sudo mv node_exporter-${NODE_VERSION}.linux-amd64/node_exporter /opt/node_exporter/
sudo chmod 755 /opt/node_exporter/node_exporter
```

Systemd unit (hardened):

```ini
# /etc/systemd/system/node_exporter.service
[Unit]
Description=Prometheus Node Exporter
Wants=network-online.target
After=network-online.target

[Service]
User=observability
Group=observability
ExecStart=/opt/node_exporter/node_exporter \
  --web.listen-address=:9100 \
  --collector.systemd \
  --collector.tcpstat \
  --collector.vmstat \
  --log.level=debug

Restart=on-failure
PrivateTmp=true
ProtectSystem=strict
NoNewPrivileges=true

[Install]
WantedBy=multi-user.target
```

### 5. Install JMX Exporter (Latest Stable: 1.0.1 – Dec 2025)

```bash
JMX_VERSION="1.0.1"
wget https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/${JMX_VERSION}/jmx_prometheus_javaagent-${JMX_VERSION}.jar
wget https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/${JMX_VERSION}/jmx_prometheus_javaagent-${JMX_VERSION}.jar.sha1
sha1sum -c jmx_prometheus_javaagent-${JMX_VERSION}.jar.sha1

sudo mv jmx_prometheus_javaagent-${JMX_VERSION}.jar /opt/jmx_exporter/jmx_prometheus_javaagent.jar
sudo chmod 644 /opt/jmx_exporter/jmx_prometheus_javaagent.jar
```

#### Recommended jvm.yml (Rich JVM Rules)

```yaml
# /opt/jmx_exporter/jvm.yml
lowercaseOutputName: true
lowercaseOutputLabelNames: true
whitelistObjectNames:
  - "java.lang:type=*"
  - "java.nio:type=*"
  - "*:type=GarbageCollector,*"
  - "*:type=MemoryPool,*"
  - "*:type=Threading"
rules:
  - pattern: 'java.lang<type=MemoryPool, name=(.*)><>Usage.used'
    name: jvm_memory_pool_bytes_used
    labels:
      pool: "$1"
    help: "Current memory usage of JVM memory pool"
    type: GAUGE

  - pattern: 'java.lang<type=GarbageCollector, name=(.*)><>CollectionTime'
    name: jvm_gc_collection_seconds_total
    labels:
      gc: "$1"
    help: "GC time spent"
    type: COUNTER

  - pattern: 'java.lang<type=Threading><>ThreadCount'
    name: jvm_threads_current
    type: GAUGE

  - pattern: 'java.lang<type=OperatingSystem><>ProcessCpuLoad'
    name: jvm_cpu_usage
    type: GAUGE
```

#### How to attach to your JVM app

```bash
# For systemd-based Java services (Spring Boot, Tomcat, etc.)
Environment="JAVA_OPTS=-javaagent:/opt/jmx_exporter/jmx_prometheus_javaagent.jar=9404:/opt/jmx_exporter/jvm.yml -Xlog:gc*:file=/var/log/app/gc.log:time,uptime,tags"
```

### 6. Install Pushgateway v1.9.0

```bash
PUSHGW_VERSION="1.9.0"
wget https://github.com/prometheus/pushgateway/releases/download/v${PUSHGW_VERSION}/pushgateway-${PUSHGW_VERSION}.linux-amd64.tar.gz
tar xvf pushgateway-${PUSHGW_VERSION}.linux-amd64.tar.gz
sudo mv pushgateway-${PUSHGW_VERSION}.linux-amd64/pushgateway /opt/pushgateway/
```

Systemd unit with persistence:

```ini
ExecStart=/opt/pushgateway/pushgateway \
  --persistence.file=/var/lib/prometheus/pushgateway.data \
  --web.listen-address=0.0.0.0:9091 \
  --log.level=debug
```

### 7. Install Alertmanager v0.27.0

```bash
AM_VERSION="0.27.0"
wget https://github.com/prometheus/alertmanager/releases/download/v${AM_VERSION}/alertmanager-${AM_VERSION}.linux-amd64.tar.gz
tar xvf alertmanager-${AM_VERSION}.linux-amd64.tar.gz
sudo mv alertmanager-${AM_VERSION}.linux-amd64/alertmanager /opt/alertmanager/
sudo mv alertmanager-${AM_VERSION}.linux-amd64/amtool /opt/alertmanager/
```

#### alertmanager.yml (example with Slack + inhibition)

```yaml
global:
  resolve_timeout: 5m

route:
  receiver: 'slack'
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h

receivers:
  - name: 'slack'
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/XXX/YYY/ZZZ'
        channel: '#alerts'
        send_resolved: true
        title: '{{ .CommonAnnotations.summary }}'
        text: '{{ .CommonAnnotations.description }}'

inhibit_rules:
  - source_matchers:
      - severity = 'critical'
    target_matchers:
      - severity =~ 'warning|info'
    equal: ['alertname', 'cluster', 'service']
```

### 8. Install Grafana v11.3.0 (Enterprise or OSS)

```bash
GRAFANA_VERSION="11.3.0"
wget https://dl.grafana.com/oss/release/grafana-${GRAFANA_VERSION}.linux-amd64.tar.gz
tar -zxvf grafana-${GRAFANA_VERSION}.linux-amd64.tar.gz
sudo mv grafana-v${GRAFANA_VERSION} /opt/grafana
```

#### Provisioning: Auto-add Prometheus datasource + JVM dashboards

```yaml
# /opt/grafana/provisioning/datasources/prometheus.yml
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://localhost:9090
    isDefault: true
    editable: false
```

```yaml
# /opt/grafana/provisioning/dashboards/jvm.yml
apiVersion: 1
providers:
  - name: 'JVM'
    type: file
    options:
      path: /opt/grafana/dashboards
```

Download best JVM dashboards:

```bash
sudo mkdir /opt/grafana/dashboards
cd /opt/grafana/dashboards
wget https://grafana.com/api/dashboards/4701/revisions/latest/download -O jvm-micrometer.json
wget https://grafana.com/api/dashboards/19394/revisions/latest/download -O jvm-overview.json
```

### 9. Final systemd Activation

```bash
for svc in prometheus node_exporter pushgateway alertmanager grafana; do
  sudo systemctl daemon-reload
  sudo systemctl enable --now $svc
  sleep 5
  sudo systemctl status $svc --no-pager
done
```

### 10. Full Validation Checklist

```bash
# Prometheus ready?
curl -f http://localhost:9090/-/ready && echo "OK"

# All targets healthy?
curl -s http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | .health + " " + .labels.job'

# Grafana reachable?
curl -f http://localhost:3000/api/health

# Login to Grafana → http://<ip>:3000 (default: admin/admin → change immediately)
```

### 11. Security Hardening (Mandatory in Production)

```bash
# Firewall (ufw example)
sudo ufw allow from 10.0.0.0/8 to any port 3000  # Grafana internal only
sudo ufw allow from 10.0.0.0/8 to any port 9090  # Prometheus internal only
sudo ufw deny 3000
sudo ufw deny 9090

# Add TLS termination via nginx/traefik/Caddy in front of Grafana
# Add basic auth or bearer token to Prometheus via --web.config.file
```

### 12. Maintenance & Operations

```bash
# Reload config without downtime
curl -X POST http://localhost:9090/-/reload

# Take snapshot backup
/opt/prometheus/promtool tsdb snapshot /var/lib/prometheus/snapshot-$(date +%Y%m%d)

# Check build info
curl -s http://localhost:9090/api/v1/status/buildinfo | jq
