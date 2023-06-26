# Workshop: Performance Profiling

## Start Dockers
Launch `docker/docker-compose.yml` to start a Postgres and a WireMock emulating response from remote APIs  

## Start the app with the Glowroot javaagent
Glowroot is a Java agent that collects performance metrics and traces.
You can download it from https://glowroot.org/
Unzip the xyz-dist.zip and copy the path to `glowroot.jar` inside.
Add Glowroot jar as a 'VM option' to your application's run configuration: `-javaagent:/path/to/glowroot.jar` 

Warning: You must use <= Java 11 for Glowroot to work.
Start the `ProfiledApp` application and check Glowroot is started at http://localhost:4000

If ok, you should see someting like:
![img.png](art/glowroot.png)

## Run the load tests
Run `LoadTest.java`.
If successful, the generated HTML report should display a green bar like this:
![img.png](art/gatling.png)

Note: We are using an artificial 'closed' load test model:
(the number of users if fixed)

## Inspect the flamegraph
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
