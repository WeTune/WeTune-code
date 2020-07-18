package sjtu.ipads.wtune.stmt.scriptgen;

import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.context.AppContext;
import sjtu.ipads.wtune.stmt.schema.Schema;
import sjtu.ipads.wtune.stmt.statement.Statement;

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
  public static void genSchema(String appName, String tag) {
    final AppContext ctx = AppContext.of(appName);
    if ("base".equals(tag)) genSchema(ctx.schema(), ctx.name(), tag);
    else genSchema(ctx.alternativeSchema(tag), ctx.name(), tag);
  }

  public static void genSchema(Schema schema, String appName, String tag) {
    genSchema(schema, Setup.current().outputDir(), appName, tag);
  }

  public static void genSchema(Schema schema, Path outputDir, String appName, String tag) {
    final Path directory = outputDir.resolve(appName);
    directory.toFile().mkdir();

    try (final PrintWriter writer =
        new PrintWriter(
            Files.newBufferedWriter(
                directory.resolve(tag + "_schema.lua"), CREATE, TRUNCATE_EXISTING))) {
      new SchemaGen(schema).output(new OutputImpl(writer));
      writer.flush();

      final Path sourcePath = schema.sourcePath();
      Files.copy(sourcePath, directory.resolve(sourcePath.getFileName()), REPLACE_EXISTING);
    } catch (IOException e) {
      throw new StmtException(e);
    }
  }

  public static void genWorkload(String appName, String tag, boolean modifySelectItem) {
    genWorkload(Statement.findByApp(appName), appName, tag, modifySelectItem);
  }

  public static void genWorkload(
      List<Statement> stmts, String appName, String tag, boolean modifySelectItem) {
    genWorkload(stmts, Setup.current().outputDir(), appName, tag, modifySelectItem);
  }

  public static void genWorkload(
      List<Statement> stmts, Path outputDir, String appName, String tag, boolean modifySelectItem) {
    final Path directory = outputDir.resolve(appName);
    directory.toFile().mkdir();

    try (final PrintWriter writer =
        new PrintWriter(
            Files.newBufferedWriter(
                directory.resolve(tag + "_workload.lua"), CREATE, TRUNCATE_EXISTING))) {
      new OutputImpl(writer).accept(new WorkloadGen(stmts, modifySelectItem));
      writer.flush();

    } catch (IOException e) {
      throw new StmtException(e);
    }
  }

  private static final List<String> RESOURCES =
      List.of(
          "exec.lua",
          "paramgen.lua",
          "parammod.lua",
          "prepare.lua",
          "randgen.lua",
          "randseq.lua",
          "sample.lua",
          "schema.lua",
          "tablegen.lua",
          "timer.lua",
          "util.lua",
          "workload.lua",
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
      final Path directory = outputDir.resolve("testbed");
      directory.toFile().mkdirs();
      Files.copy(input, directory.resolve(fileName), REPLACE_EXISTING);
    } catch (IOException e) {
      throw new StmtException(e);
    }
  }
}
