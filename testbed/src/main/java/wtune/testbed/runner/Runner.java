package wtune.testbed.runner;

import java.nio.file.Path;

public interface Runner {
  void prepare(String[] argStrings) throws Exception;

  void run() throws Exception;

  default void stop() throws Exception {}

  static Path dataDir() {
    return Path.of(System.getProperty("wetune.data_dir", "wtune_data"));
  }

  static int parseIntArg(String str, String argName) {
    try {
      return Integer.parseInt(str);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException(
          "invalid '-%s': %s, integer required".formatted(argName, str));
    }
  }

  static int parseIntSafe(String str, int onFailure) {
    try {
      return Integer.parseInt(str);
    } catch (NumberFormatException ex) {
      return onFailure;
    }
  }
}
