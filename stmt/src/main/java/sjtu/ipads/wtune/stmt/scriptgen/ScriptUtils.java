package sjtu.ipads.wtune.stmt.scriptgen;

import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.context.AppContext;
import sjtu.ipads.wtune.stmt.schema.Schema;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

public class ScriptUtils {

  public static void genSchema(AppContext ctx) {
    genSchema(ctx.schema(), ctx.name());
  }

  public static void genSchema(Schema schema, String appName) {
    genSchema(schema, Setup.current().outputDir(), appName);
  }

  public static void genSchema(Schema schema, Path outputDir, String appName) {
    final Path directory = outputDir.resolve(appName);
    directory.toFile().mkdir();

    try (final PrintWriter writer =
        new PrintWriter(
            Files.newBufferedWriter(
                directory.resolve("base_schema.lua"), CREATE, TRUNCATE_EXISTING))) {
      new SchemaGen(schema).output(new OutputImpl(writer));
      writer.flush();

    } catch (IOException e) {
      throw new StmtException(e);
    }
  }

  private static final List<String> RESOURCES =
      List.of(
          "prepare.lua",
          "randgen.lua",
          "randseq.lua",
          "schema.lua",
          "tablegen.lua",
          "util.lua",
          "wtune.lua");

  public static void copyResources() {
    copyResources(Setup.current().outputDir());
  }

  public static void copyResources(Path outputDir) {
    RESOURCES.forEach(it -> copyResource(outputDir, it));
  }

  private static void copyResource(Path outputDir, String fileName) {
    final InputStream input = ScriptUtils.class.getResourceAsStream("/testbed/" + fileName);
    assert input != null;
    try {
      Files.copy(input, outputDir.resolve("testbed").resolve(fileName), REPLACE_EXISTING);
    } catch (IOException e) {
      throw new StmtException(e);
    }
  }

  public static void main(String[] args) {
    Setup._default().registerAsGlobal();
    ScriptUtils.copyResources();
    ScriptUtils.genSchema(AppContext.of("broadleaf"));
  }
}
