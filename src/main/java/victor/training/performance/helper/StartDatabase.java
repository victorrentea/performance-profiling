package victor.training.performance.helper;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public class StartDatabase {
	public static void main(String[] args) throws SQLException, IOException {
		System.out.println("Starting DB...");
		deletePreviousDB();
		org.h2.tools.Server.createTcpServer("-ifNotExists").start();
		System.out.println("✅ DB Started. You can connect to it using sa:sa at jdbc:h2:tcp://localhost:9092/~/test");

		System.out.println("\nStarting DB Proxy ...");
		StartDatabaseProxy proxy = new StartDatabaseProxy("localhost", 9092, 19092, 5);
		CompletableFuture.runAsync(proxy::run);
		System.out.println("✅ DB Proxy Started. Traffic delayed to jdbc:h2:tcp://localhost:19092/~/test");
	}

	private static void deletePreviousDB() {
		File dbFile = new File(System.getProperty("user.home"), "test.mv.db");
		System.out.println("Db file: " +dbFile.getAbsolutePath());

		if (dbFile.isFile()) {
			boolean r = dbFile.delete();
			if (!r) {
				System.err.println("Could not delete previous db content!");
			} else {
				System.out.println("Previous DB contents wiped out");
			}
		}
	}
}
