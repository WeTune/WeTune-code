package sjtu.ipads.wtune.bootstrap;

import sjtu.ipads.wtune.stmt.Setup;
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

  private PrintWriter statOut;
  private PrintWriter optStmtOut;

  private void prepareOutput(String appName) {
    final Path dir = Setup.current().outputDir();
    final boolean append = target != -1 || continueFrom != -1;
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
    for (int i = 0; i < optimized.size(); i++) {
      final Statement opt = optimized.get(i);
      final long baseP50 = output.baseP50;
      final Long optP50 = output.optP50.get(i);
      final double improvement = (double) (baseP50 - optP50) / baseP50;
      optStmtOut.printf(
          "%s,%d,%d,%d,%d,%f,\"%s\"\n",
          output.base.appName(),
          output.base.stmtId(),
          i,
          baseP50,
          optP50,
          improvement,
          opt.parsed().toString().replaceAll("\"", "\"\""));
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
  }

  @Override
  public void doTask(String appName) {
    prepareOutput(appName);

    final List<Integer> failed = new ArrayList<>();
    for (Statement stmt : fastRecycleIter(Statement.findByApp(appName))) {
      if (stmt.stmtId() < continueFrom) continue;
      if (target != -1 && target != stmt.stmtId()) continue;

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
}
