package sjtu.ipads.wtune.stmt.scriptgen;

import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.sqlparser.schema.SchemaPatch;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.dao.SchemaPatchDao;
import sjtu.ipads.wtune.stmt.support.Workflow;
import sjtu.ipads.wtune.stmt.utils.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static sjtu.ipads.wtune.common.utils.Commons.coalesce;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.stmt.Statement.TAG_OPT;

public class ScriptUtils {
  public static void genSchema(String appName, String tag) {
    final App ctx = App.of(appName);
    genSchema(ctx.schema(tag), ctx.name(), tag);
  }

  public static void genSchema(Schema schema, String appName, String tag) {
    genSchema(schema, FileUtils.testScriptDir(), appName, tag);
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

      //      final Path sourcePath = schema.sourcePath();
      //      Files.copy(sourcePath, directory.resolve(sourcePath.getFileName()), REPLACE_EXISTING);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void genWorkload(String appName, String tag) {
    List<Statement> stmts = Statement.findByApp(appName);
    if ("opt".equals(tag)) stmts = listMap(it -> coalesce(it.alternative(TAG_OPT), it), stmts);
    stmts.forEach(Workflow::retrofit);
    genWorkload(stmts, FileUtils.testScriptDir(), appName, tag);
  }

  public static void genWorkload(
      List<Statement> stmts, Path outputDir, String appName, String tag) {
    final Path directory = outputDir.resolve(appName);
    directory.toFile().mkdir();

    try (final PrintWriter writer =
        new PrintWriter(
            Files.newBufferedWriter(
                directory.resolve(tag + "_workload.lua"), CREATE, TRUNCATE_EXISTING))) {
      new OutputImpl(writer).accept(new WorkloadGen(stmts));
      writer.flush();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void genSchemaPatch(String appName) {
    genSchemaPatch(SchemaPatchDao.instance().findByApp(appName), appName);
  }

  public static void genSchemaPatch(List<SchemaPatch> patches, String appName) {
    genSchemaPatch(patches, FileUtils.testScriptDir(), appName);
  }

  public static void genSchemaPatch(List<SchemaPatch> patches, Path outputDir, String appName) {
    final Path directory = outputDir.resolve(appName);
    directory.toFile().mkdir();

    try (final PrintWriter writer =
        new PrintWriter(
            Files.newBufferedWriter(directory.resolve("patch.sql"), CREATE, TRUNCATE_EXISTING))) {
      new OutputImpl(writer).accept(new PatchesGen(patches, true));
      writer.flush();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try (final PrintWriter writer =
        new PrintWriter(
            Files.newBufferedWriter(directory.resolve("unpatch.sql"), CREATE, TRUNCATE_EXISTING))) {
      new OutputImpl(writer).accept(new PatchesGen(patches, false));
      writer.flush();

    } catch (IOException e) {
      throw new RuntimeException(e);
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
    copyResources(FileUtils.testScriptDir().resolve("testbed"));
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
      throw new RuntimeException(e);
    }
  }
}
