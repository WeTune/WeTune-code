package sjtu.ipads.wtune.bootstrap;

import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.statement.Issue;
import sjtu.ipads.wtune.stmt.statement.Statement;
import sjtu.ipads.wtune.systhesis.Synthesis;
import sjtu.ipads.wtune.systhesis.SynthesisOutput;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static sjtu.ipads.wtune.bootstrap.FastRecycleStmtIterator.fastRecycleIter;

public class SynthesisAll implements Task {
  private static OutputStream prepareOutput(Path dir, String fileName, boolean append)
      throws IOException {
    final Path filePath = dir.resolve(fileName);
    if (Files.exists(filePath))
      Files.copy(filePath, dir.resolve(fileName + ".old"), StandardCopyOption.REPLACE_EXISTING);

    return Files.newOutputStream(
        filePath,
        StandardOpenOption.CREATE,
        append ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE);
  }

  private int target = -1;
  private int continueFrom = -1;
  private boolean checkKnown = false;

  private PrintWriter statOut;
  private PrintWriter optStmtOut;

  private void prepareOutput(String appName, boolean append) {
    final Path dir = Setup.current().outputDir();
    try {
      statOut =
          new PrintWriter(
              new BufferedOutputStream(prepareOutput(dir, appName + "-synthesis.out", append)));
      optStmtOut =
          new PrintWriter(
              new BufferedOutputStream(prepareOutput(dir, appName + "-optstmt.out", append)));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void closeOutput() {
    statOut.close();
    optStmtOut.close();
  }

  private void outputStat(SynthesisOutput output) {
    statOut.printf(
        "%s,%d,%d,%d,%d,%d,%d,%d,%d,%d\n",
        output.base.appName(),
        output.base.stmtId(),
        output.totalRefCount,
        output.usedRefCount,
        output.producedCount,
        output.relationElapsed,
        output.predicateElapsed,
        output.exprListElapsed,
        output.verificationElapsed,
        output.totalElapsed());
  }

  private void outputOpt(SynthesisOutput output) {
    final List<Statement> optimized = output.optimized;
    final List<Integer> ranking = output.ranking;
    for (int i = 0; i < optimized.size(); i++) {
      final Statement opt = optimized.get(i);
      final Integer r = ranking.get(i);
      final long baseP50 = output.baseP50;
      final Long optP50 = output.optP50.get(i);
      final double improvement = (double) (baseP50 - optP50) / baseP50;
      optStmtOut.printf(
          "%s,%d,%d,%d,%d,%f,\"%s\",%d\n",
          output.base.appName(),
          output.base.stmtId(),
          i,
          baseP50,
          optP50,
          improvement,
          opt.parsed().toString().replaceAll("\"", "\"\""),
          r);
    }
  }

  private void flushOutput() {
    statOut.flush();
    optStmtOut.flush();
  }

  @Override
  public void setArgs(String... args) {
    for (String arg : args)
      if (arg.startsWith("--continue="))
        continueFrom = Integer.parseInt(arg.substring("--continue=".length()));
      else if (arg.startsWith("--target="))
        target = Integer.parseInt(arg.substring("--target=".length()));
      else if (arg.startsWith("--checkKnown")) checkKnown = true;
  }

  public void doTask0(String appName, List<Statement> stmts, boolean appendOutput) {
    prepareOutput(appName, appendOutput);

    final List<Integer> failed = new ArrayList<>();
    for (Statement stmt : fastRecycleIter(stmts)) {
      try {
        final SynthesisOutput output = Synthesis.synthesis(stmt);
        if (output.optimized.isEmpty()) continue;
        outputStat(output);
        outputOpt(output);
        flushOutput();

      } catch (Exception ex) {
        failed.add(stmt.stmtId());
      }
    }

    closeOutput();

    for (Integer id : failed) System.out.printf("failed: %s-%d\n", appName, id);
  }

  @Override
  public void doTask(String appName) {
    if (target != -1) {
      final List<Statement> stmt = new ArrayList<>();
      stmt.add(Statement.findOne(appName, target));
      doTask0(appName, stmt, true);
    } else if (checkKnown) {
      doTask0(
          appName,
          Issue.findByApp(appName).stream()
              .dropWhile(it -> it.stmtId() < continueFrom)
              .map(Issue::stmt)
              .collect(Collectors.toList()),
          false);

    } else {
      doTask0(
          appName,
          Statement.findByApp(appName).stream()
              .dropWhile(it -> it.stmtId() < continueFrom)
              .collect(Collectors.toList()),
          continueFrom != -1);
    }
  }
}
