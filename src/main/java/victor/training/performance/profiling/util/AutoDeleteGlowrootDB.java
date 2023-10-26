package victor.training.performance.profiling.util;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Map;

@Slf4j
@Component
public class AutoDeleteGlowrootDB {
  @Autowired
  private RestTemplate rest;

//  @EventListener(ApplicationStartedEvent.class)
  @PreDestroy
  public void atEnd() throws IOException {
    // otherwise Glowroot preserves its data over a restart
    String glowrootPath = ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
            .filter(jvmArg -> jvmArg.startsWith("-javaagent:") && jvmArg.endsWith("glowroot.jar"))
            .map(jvmArg -> jvmArg.replace("-javaagent:", "").replace("glowroot.jar", ""))
            .findFirst()
            .orElse(null);
    if (glowrootPath == null) {
      log.warn("Glowroot agent not found: no database to wipe out");
      return;
    }
    log.debug("Glowroot agent found running from path: " + glowrootPath);
    File dataDir = new File(glowrootPath + "data");
    if (dataDir.isDirectory()) {

//      FileUtils.deleteDirectory(dataDir);
      rest.postForObject("http://localhost:4000/backend/admin/delete-all-stored-data", Map.of(), String.class);
      log.info("Glowroot DB successfully cleared");
    }
  }
}
