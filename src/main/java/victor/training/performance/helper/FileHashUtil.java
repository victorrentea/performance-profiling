package victor.training.performance.helper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class FileHashUtil {

  public static String computeShortHash(Path filePath) throws IOException, NoSuchAlgorithmException {
    return computeShortHash(filePath, 10);
  }

  public static String computeShortHash(Path filePath, int maxLength) throws IOException, NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] fileBytes = Files.readAllBytes(filePath);
    byte[] hashBytes = digest.digest(fileBytes);
    String fullHash = HexFormat.of().formatHex(hashBytes);
    return fullHash.substring(0, Math.min(maxLength, fullHash.length()));
  }
}
