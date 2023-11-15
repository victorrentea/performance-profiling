# Profiling with Java Flight Recorder

## Setup

### Start a Database
You can use an in-memory standalone H2 or a PostgreSQL in a Docker.
#### A) Standalone H2
Run `StartDatabase.java` to start the H2 database server. 
Make sure you use option (GB-H2) in the application.properties file.
(url = `jdbc:h2:tcp://localhost/~/test`)

#### B) Postgres in Docker
If you have a Docker Desktop installed on your machine:
- Start Postgres using the `docker/docker-compose.yml`
- Use option (DB-PG) in the application.properties file.

### Add DB Latency😴
To simulate network delay in talking to a DB running on the same machine, choose ONE of the options below.
#### A) Hibernate Interceptor
Use `SimulateNetworkDelayHibernateInterceptor` in the properties files to add a fixed delay to any prepared statement 
created by Hibernate.
#### B) ToxiProxy
Emulate network latency by driving all DB traffic through a proxy delaying network packets. 
- Install [ToxiProxy](https://github.com/Shopify/toxiproxy#1-installing-toxiproxy) to your local machine.
- Check it's started on port 8474 - you should see a 404 page at [http://localhost:8474](http://localhost:8474) 
- Run `ConfigureToxiproxy`
- Change DB Port in application properties to point to the proxy port (eg) 5432 -> 55432


### WireMock to simulate API calls with delay
The WireMock stubs are in `src/test/wiremock/mappings` folder.
#### A) Standalone Java Program
Run `StartWireMock.java` to start the WireMock server.
#### B) In Docker
Start 'wiremock' image in docker-compose.yml


### Start your app with Glowroot agent
Glowroot is a lightweight Java agent that collects performance metrics and traces.
Download it from [glowroot.org](https://glowroot.org/).
Unzip the dist zip, locate the `glowroot.jar` and copy the path to it.

Add glowroot.jar as a java agent to your application by adding `-javaagent:/path/to/glowroot.jar`.
You can do that in IntelliJ by filling the 'VM option' field of your run configuration.

When you run the application, you can access the Glowroot UI at http://localhost:4000. You should see a page like this:
![img.png](art/glowroot.png)

## Start the application
Run `ProfiledApp.java` with the Glowroot agent.

## Run the load tests
Run `LoadTest.java` and click the report printed in the console at the end.
If successful, the generated HTML report should display a green bar like this:
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



Import https://grafana.com/grafana/dashboards/19004-spring-boot-statistics/