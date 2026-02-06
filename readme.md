# Java Performance Profiling Workshop

## Setup

### 1. Start the Database
- [▶️ Run StartDatabase](src/main/java/victor/training/performance/helper/StartDatabase.java) to start a standalone in-memory H2 database at `jdbc:h2:tcp://localhost:9092/~/test` (user=`sa`, password=`sa`)
- Traffic is delayed by a [network proxy](src/main/java/victor/training/performance/helper/NetworkLatencyProxy.java) on port `19092`
- (Optional) Connect from IntelliJ through the proxy: `jdbc:h2:tcp://localhost:19092/~/test`

### 2. Start the Second App
- [▶️ Run SecondApp](src/main/java/victor/training/performance/helper/SecondApp.java) to start the downstream service

### 3. Start the Profiled App
- [▶️ Run ProfiledApp](src/main/java/victor/training/performance/profiling/ProfiledApp.java) to start the main application

### 4. Instrument with Glowroot
Glowroot is a lightweight Java Agent for performance metrics and profiling.
- Download from [glowroot.org](https://glowroot.org/)
- Add to VM options: `-javaagent:/path/to/glowroot.jar`
- Open UI: http://localhost:4000

![Glowroot UI](art/glowroot.png)

### 5. Run Load Tests
- [▶️ Run LoadTest](src/test/java/LoadTest.java) and click the generated report link
- Study the flamegraph: http://localhost:4000/transaction/thread-flame-graph?transaction-type=Web

![Gatling Report](art/gatling.png)

## Optimization Steps

### 1. Avoid Useless Network Call from @Aspect
- `restTemplate.getForObject` sometimes runs unnecessarily: reorder guard clauses
- Observe: aspect execution time eliminated

### 2. Fix JDBC Connection Starvation
- Observe: Hikari `getConnection` time in flamegraph
- (Optional) Increase Hikari pool size → starvation fixed; UNDO
- Remove `@Transactional` from `getLoanApplication` → no change
- Release connections earlier: `spring.jpa.open-in-view=false`

### 3. Fix Lazy Loading in toString
- Observe: `LoanApplication.toString` triggers lazy load
- Solutions:
  1. Use `log.trace("... {}", loanApplication)` instead of string concatenation
  2. Add `@ToString.Exclude` on collection fields or create manual `toString`

### 4. Fix Apache HTTP Client Connection Pool
- Observe: time spent acquiring connection from pool
- Remove `feign.httpclient.max-connections-per-route` from `application.properties`

## Optional: OpenTelemetry Instrumentation

Requires local Docker.

1. Start monitoring: `docker-compose -f grafana-otel-lgtm.yml up`
2. Download OTEL agent: [opentelemetry-java-instrumentation releases](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases)
3. Add to VM options:
   ```
   -javaagent:/path/to/opentelemetry-javaagent.jar
   -Dotel.instrumentation.micrometer.enabled=true
   -Dotel.metric.export.interval=500
   -Dotel.bsp.schedule.delay=500
   ```
4. Import Grafana dashboard: https://grafana.com/grafana/dashboards/19004-spring-boot-statistics/
