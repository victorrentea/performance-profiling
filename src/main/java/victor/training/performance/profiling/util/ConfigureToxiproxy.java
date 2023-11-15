package victor.training.performance.profiling.util;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;

import java.io.IOException;

public class ConfigureToxiproxy {

  public static void main(String[] args) throws IOException {
    ToxiproxyClient client = new ToxiproxyClient("localhost", 8474);

    for (Proxy proxy : client.getProxies()) proxy.delete();

    // toxiproxy inside Docker sees requests to its machine as coming to toxiproxy host
    var pg = client.createProxy("pg", "toxiproxy:55432", "postgres:5432");

    pg.toxics().latency("real-latency", ToxicDirection.DOWNSTREAM, 5);

//    var wm = client.createProxy("wm", "toxiproxy:9991", "wiremock:9999");

    System.out.println("Configured Toxiproxy");
  }

}
