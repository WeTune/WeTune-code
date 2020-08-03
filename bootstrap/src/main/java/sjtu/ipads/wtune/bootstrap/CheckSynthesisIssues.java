package sjtu.ipads.wtune.bootstrap;

import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.analyzer.TableAccessAnalyzer;
import sjtu.ipads.wtune.stmt.schema.Table;
import sjtu.ipads.wtune.stmt.statement.Issue;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CheckSynthesisIssues implements Task {
  private String profile;
  private boolean recreateDb;
  private boolean runBase;
  private boolean runOpt;

  @Override
  public void setArgs(String... args) {
    for (String arg : args)
      if (arg.startsWith("--profile=")) profile = arg.substring("--profile=".length());
      else if (arg.startsWith("--recreate")) recreateDb = true;
      else if (arg.startsWith("--base")) runBase = true;
      else if (arg.startsWith("--opt")) runOpt = true;
  }

  @Override
  public void doTask(String appName) {
    final List<Statement> stmts =
        Issue.findByApp(appName).stream().map(Issue::stmt).collect(Collectors.toList());
    if (stmts.isEmpty()) {
      System.out.println("nothing to run for " + appName);
      return;
    }
    stmts.forEach(Statement::retrofitStandard);

    final String stmtIds = collectStmtIds(stmts);
    final String tableNames = collectTableNames(stmts);
    try {
      if (recreateDb) {
        run(appName, "base" + profile, "recreate", true);
        run(appName, "base" + profile, "prepare", true, "-T", tableNames);
      }
      if (runBase) run(appName, "base" + profile, "eval", true, "-o", "-T", stmtIds);
      if (runOpt) run(appName, "opt" + profile, "eval", true, "-o", "-T", stmtIds);

    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static String collectStmtIds(List<Statement> stmts) {
    return stmts.stream()
        .map(Statement::stmtId)
        .map(String::valueOf)
        .collect(Collectors.joining(","));
  }

  private static String collectTableNames(List<Statement> stmts) {
    return stmts.stream()
        .map(it -> it.analyze(TableAccessAnalyzer.class))
        .flatMap(Collection::stream)
        .distinct()
        .map(Table::tableName)
        .collect(Collectors.joining(","));
  }

  private static void run(
      String appName, String profile, String command, boolean output, String... args)
      throws IOException, InterruptedException {
    final String[] realArgs = new String[args.length + 7];
    realArgs[0] = "python3";
    realArgs[1] = "exec.py";
    realArgs[2] = "-p";
    realArgs[3] = profile;
    realArgs[4] = "-c";
    realArgs[5] = command;
    realArgs[6] = appName;
    System.arraycopy(args, 0, realArgs, 7, args.length);

    final Process p =
        new ProcessBuilder(realArgs).directory(Setup.current().outputDir().toFile()).start();

    if (output)
      try (final Reader reader = new InputStreamReader(p.getInputStream())) {
        int c;
        while ((c = reader.read()) != -1) System.out.print(Character.valueOf((char) c));
      }
    p.waitFor();
  }
}
