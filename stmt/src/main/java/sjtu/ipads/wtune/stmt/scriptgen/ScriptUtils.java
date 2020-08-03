package sjtu.ipads.wtune.stmt.scriptgen;

import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.context.AppContext;
import sjtu.ipads.wtune.stmt.mutator.SelectItemNormalizer;
import sjtu.ipads.wtune.stmt.resolver.ColumnResolver;
import sjtu.ipads.wtune.stmt.resolver.ParamResolver;
import sjtu.ipads.wtune.stmt.schema.Schema;
import sjtu.ipads.wtune.stmt.schema.SchemaPatch;
import sjtu.ipads.wtune.stmt.schema.Table;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static sjtu.ipads.wtune.common.utils.FuncUtils.coalesce;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.stmt.statement.Statement.TAG_OPT;

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

  public static void genWorkload(String appName, String tag) {
    List<Statement> stmts = Statement.findByApp(appName);
    //    if ("index".equals(tag)) stmts = listMap(it -> coalesce(it.alt(TAG_INDEX), it), stmts);
    if ("opt".equals(tag)) stmts = listMap(it -> coalesce(it.alt(TAG_OPT), it), stmts);
    for (Statement stmt : stmts) {
      stmt.retrofitStandard();
      stmt.mutate(SelectItemNormalizer.class);
      stmt.resolve(ColumnResolver.class, true);
    }
    genWorkload(stmts, appName, tag);
  }

  public static void genWorkload(List<Statement> stmts, String appName, String tag) {
    stmts.forEach(it -> it.resolve(ParamResolver.class));
    genWorkload(stmts, Setup.current().outputDir(), appName, tag);
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
      throw new StmtException(e);
    }
  }

  public static void genSchemaPatch(String appName) {
    final List<SchemaPatch> patches =
        SchemaPatch.findByApp(appName).stream()
            .filter(it -> !"manual".equals(it.source()))
            .collect(Collectors.toList());
    genSchemaPatch(patches, appName);
  }

  public static void genSchemaPatch(List<SchemaPatch> patches, String appName) {
    genSchemaPatch(patches, Setup.current().outputDir(), appName);
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
      throw new StmtException(e);
    }

    try (final PrintWriter writer =
        new PrintWriter(
            Files.newBufferedWriter(directory.resolve("unpatch.sql"), CREATE, TRUNCATE_EXISTING))) {
      new OutputImpl(writer).accept(new PatchesGen(patches, false));
      writer.flush();

    } catch (IOException e) {
      throw new StmtException(e);
    }
  }

  public static void genEngineChange(Map<Table, String> changes, String appName) {
    genEngineChange(changes, Setup.current().outputDir(), appName);
  }

  public static void genEngineChange(Map<Table, String> changes, Path outputDir, String appName) {
    final Path directory = outputDir.resolve(appName);
    directory.toFile().mkdir();

    try (final PrintWriter writer =
        new PrintWriter(
            Files.newBufferedWriter(directory.resolve("engine.sql"), CREATE, TRUNCATE_EXISTING))) {
      for (var pair : changes.entrySet())
        writer.printf("%s.%s -> %s\n", appName, pair.getKey().tableName(), pair.getValue());

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
