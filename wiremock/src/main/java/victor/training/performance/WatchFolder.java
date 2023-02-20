package victor.training.performance;

import com.sun.nio.file.SensitivityWatchEventModifier;

import java.io.File;
import java.nio.file.*;
import java.util.concurrent.Callable;

import static java.nio.file.StandardWatchEventKinds.*;

public class WatchFolder {

  public static void watchFolder(File directoryFile, Callable<Object> callback) {
    try {
      System.out.println("Watching directory for changes: " + directoryFile.getAbsolutePath());

      Path directory = directoryFile.toPath();

      if (!directory.toFile().isDirectory()) {
        throw new IllegalArgumentException("Not a folder: " + directory.toFile().getAbsolutePath());
      }

      WatchService watchService = FileSystems.getDefault().newWatchService();
      WatchKey watchKey = directory.register(watchService,
              new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE},
              SensitivityWatchEventModifier.HIGH);

      while (true) {
        for (WatchEvent<?> event : watchKey.pollEvents()) {
          WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
          try {
              System.out.println("A file has changed : " + pathEvent.context());
              callback.call();
          } catch (Exception e) {
            System.err.println("Callback failed: " + e);
          }
        }

        // STEP8: Reset the watch key everytime for continuing to use it for further event polling
        boolean valid = watchKey.reset();
        if (!valid) {
          break;
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

}