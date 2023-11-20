
## Setup

1. clone & mvn install
2. import in IDE
3. start docker-compose.yml (You have a Docker Desktop installed on your machine)
4. Add 5ms DB Latency via a ToxiProxy: run `ConfigureToxiproxy`
   a. Change DB Port in application properties to point to the ToxyProxi-ed port (eg) 5432 -> 55432
5. Download glowroot from [glowroot.org](https://glowroot.org/). Unzip the dist zip, locate the `glowroot.jar` and copy the path to it.
6. Add glowroot.jar as a java agent to the run config of ProfiledApp by adding `-javaagent:/path/to/glowroot.jar`.
   You can do that in IntelliJ by filling the 'VM option' field of your run configuration.
7. Run `ProfiledApp` 
   a. Check glowroot works at http://localhost:4000. You should see a page like this:
   ![img.png](art/glowroot.png)
8. Run `LoadTest.java` and click the link to the report printed in the console at the end. 
   aIf successful, the generated HTML report should display a green bar like this:
   ![img.png](art/gatling.png)
9. See the profiler results as a flamegraph at http://localhost:4000/transaction/thread-flame-graph?transaction-type=Web

## Optimization steps
1. Avoid useless network call from @Aspect
   - restTemplate.getForObject sometimes does not have to run: reorder lines
   - Observe: the time spent in the aspect is gone
2. Fix JDBC Connection Starvation issue:
   - Observe the Hikari getConnection time in the flamegraph
   - [Optional] increase Hikari connection pool size -> starvation fixed; UNDO
   - Make getLoanApplication not @Transactional -> no change :( 
   - Release the JDBC Connection earlier by `spring.jpa.open-in-view=false`
3. Lazy loading in toString
   - Observe: LoanApplication.toString performs a lazy load
   1) Use log.trace("... {}", loanApplication) instead of log.trace("..." + loanApplication)
   2) Fix the lazy load by manually creating a toString that does not include the collection fields/@ToString.Exclude
4. Fix the Apache HTTP Client connection pool
   - Observe: time is spent to acquire a connection from the Apache Http connection pool
   - Remove `feign.httpclient.max-connections-per-route` from application.properties



Import https://grafana.com/grafana/dashboards/19004-spring-boot-statistics/