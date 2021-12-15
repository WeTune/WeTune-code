package sjtu.ipads.wtune.common.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.function.Consumer;

public interface IOSupport {
  static void printWithLock(Path path, Consumer<PrintWriter> writer) {
    try (final var os = new FileOutputStream(path.toFile(), true);
        final var out = new PrintWriter(os);
        final var lock = os.getChannel().lock(); ) {
      writer.accept(out);
    } catch (IOException ioe) {
      throw new UncheckedIOException(ioe);
    }
  }
}
