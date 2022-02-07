package sjtu.ipads.wtune.superopt.runner;

import sjtu.ipads.wtune.common.utils.Args;
import sjtu.ipads.wtune.common.utils.IOSupport;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.support.OptimizerType;
import sjtu.ipads.wtune.stmt.support.UpdateStmts;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static sjtu.ipads.wtune.superopt.runner.RunnerSupport.dataDir;
import static sjtu.ipads.wtune.superopt.runner.RunnerSupport.parseIntSafe;

public class UpdateOptStmts implements Runner {
  private Path optFile;
  private int verbosity;
  private String optimizedBy;

  @Override
  public void prepare(String[] argStrings) throws Exception {
    final Args args = Args.parse(argStrings, 1);

    verbosity = args.getOptional("v", "verbose", int.class, 0);
    optimizedBy = args.getOptional("opt", "optimizer", String.class, "WeTune");

    final Path dataDir = dataDir();
    final Path dir = dataDir.resolve(args.getOptional("D", "dir", String.class, "rewrite/result"));
    optFile = dir.resolve(args.getOptional("i", "in", String.class, "2_query.tsv"));
    IOSupport.checkFileExists(optFile);
  }

  @Override
  public void run() throws Exception {
    final List<String> lines = Files.readAllLines(optFile);
    final OptimizerType optimizerType = OptimizerType.valueOf(optimizedBy);

    UpdateStmts.cleanOptStmts(optimizerType);
    for (int i = 0, bound = lines.size(); i < bound; i++) {
      final String line = lines.get(i);
      final String[] fields = line.split("\t", 4);
      if (fields.length != 4) {
        if (verbosity >= 1) System.err.println("malformed line " + i + " " + line);
        continue;
      }
      final String app = fields[0];
      final int stmtId = parseIntSafe(fields[1], -1);
      final String rawSql = fields[2], trace = fields[3];
      if (app.isEmpty() || stmtId <= 0) {
        if (verbosity >= 1) System.err.println("malformed line " + i + " " + line);
        continue;
      }
      UpdateStmts.updateOptStmts(Statement.mk(app, stmtId, rawSql, trace), optimizerType);
    }
  }
}
