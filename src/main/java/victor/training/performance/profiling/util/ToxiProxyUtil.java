package victor.training.performance.profiling.util;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;

import java.io.IOException;

public class ToxiProxyUtil {

  public static final String PG_PROXY = "toxi-proxy";

  public static void main(String[] args) throws IOException {
    delayTrafficToPostgres();
  }

  public static void delayTrafficToPostgres() throws IOException {
    // for this to work, please install ToxiProxy locally: https://github.com/shopify/toxiproxy#usage
    // eg on macos:
    // - brew install toxiproxy
    // - brew services start shopify/shopify/toxiproxy
    ToxiproxyClient client = new ToxiproxyClient("localhost", 8474);

    Proxy oldProxy = client.getProxyOrNull(PG_PROXY);
    if (oldProxy != null) {
      oldProxy.delete();
      System.out.println("Deleted old Toxiproxy");
    }

    // Note: if using other DB than Postgres, edit the port binding below:
    String addressOfRealDB = "localhost:5432";
    String addressListenedByProxy = "localhost:55432";

    Proxy proxy = client.createProxy(PG_PROXY, addressListenedByProxy, addressOfRealDB);
    System.out.println("Created Toxiproxy");

    proxy.toxics().latency("real-latency", ToxicDirection.DOWNSTREAM, 3);
    System.out.println("Configured Toxiproxy");
  }

}
