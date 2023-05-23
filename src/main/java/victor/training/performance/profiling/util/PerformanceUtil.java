package victor.training.performance.profiling.util;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.System.currentTimeMillis;

public class PerformanceUtil {

    public static void printJfrFile() {
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMxBean.getInputArguments();
        Optional<String> jfrArg = arguments.stream()
//              .filter(a -> a.contains("StartFlightRecording"))
                .filter(a -> a.contains("jfr"))
                .findFirst();
        if (jfrArg.isPresent()) {
            String jfrArgValue = jfrArg.get();
            if (jfrArgValue.contains("file=")) {
                Matcher matcher = Pattern.compile("[^\"=]+.jfr").matcher(jfrArgValue.substring(jfrArgValue.indexOf("file=")));
                if (matcher.find()) {
                    System.out.println("Recording JFR in file: " + matcher.group(0));
                    long t0 = currentTimeMillis();
                    Runtime.getRuntime().addShutdownHook(new Thread() {
                        @Override
                        public void run() {
                            System.out.println("Program ended. Recorded JFR for " + (currentTimeMillis() - t0) + " millis in file: " + matcher.group(0));
                        }
                    });
                    return;
                }
            }
        }
        System.out.println("<JFR not started>");
    }

    /**
     * Sleeps quietly (without throwing a checked exception)
     */
    public static void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

}
