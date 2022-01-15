package sjtu.ipads.wtune.superopt.runner;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.nio.file.Files;
import java.nio.file.Path;

abstract class RunnerSupport {
  private RunnerSupport() {}

  static TIntSet parseIndices(String spec) {
    final TIntSet indices = new TIntHashSet();
    final String[] ranges = spec.split(",");
    for (String range : ranges) {
      if (range.isEmpty()) {
        throw new IllegalArgumentException("invalid index range: " + spec);
      }

      final String[] fields = range.split("-");

      try {
        if (fields.length == 1) {
          indices.add(Integer.parseInt(fields[0]));
        } else if (fields.length == 2) {
          final int begin = Integer.parseInt(fields[0]);
          final int end = Integer.parseInt(fields[1]);
          for (int i = begin; i < end; ++i) indices.add(i);
        }
        continue;

      } catch (NumberFormatException ignored) {
      }

      throw new IllegalArgumentException("invalid index range: " + spec);
    }
    return indices;
  }

  static void checkFileExists(Path path) {
    if (!Files.exists(path)) throw new IllegalArgumentException("no such file: " + path);
  }

  static int parseIntArg(String str, String argName) {
    try {
      return Integer.parseInt(str);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException(
          "invalid '-%s': %s, integer required".formatted(argName, str));
    }
  }
}
