# Workshop: Performance Profiling

## Start Dockers
If you have a Docker Desktop installed on your machine:
- Start Postgres + WireMock using the `docker/docker-compose.yml`

## Glowroot
Glowroot is a Java agent that collects performance metrics and traces.
You can download it from glowroot.org
Unzip the dist and copy the path to `glowroot.jar` inside.

Add Glowroot as a 'VM option' to your application: `-javaagent:/path/to/glowroot.jar` in the run configuration

You must use <= Java 11 for Glowroot to work.
When you run the application, you can access the Glowroot UI at http://localhost:4000
![img.png](art/glowroot.png)

## Start the application
Run `ProfiledApp.java` with the Glowroot agent.

## Run the load tests
Run `LoadTest.java` and if successful, the generated HTML
report should display a green bar like this:
![img.png](art/gatling.png)

Note: We are using an artificial 'closed' load test model:
(the number of users if fixed)

## See the flamegraph
Go to http://localhost:4000/transaction/thread-flame-graph?transaction-type=Web

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
