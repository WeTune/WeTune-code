package sjtu.ipads.wtune.superopt.runner;

import gnu.trove.set.TIntSet;
import sjtu.ipads.wtune.common.utils.Args;
import sjtu.ipads.wtune.sql.SqlSupport;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.sql.plan.PlanContext;
import sjtu.ipads.wtune.sql.plan.PlanSupport;
import sjtu.ipads.wtune.sql.schema.Schema;
import sjtu.ipads.wtune.sql.support.action.NormalizationSupport;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.superopt.optimizer.Optimizer;
import sjtu.ipads.wtune.superopt.optimizer.OptimizerSupport;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionBank;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.sql.SqlSupport.parseSql;
import static sjtu.ipads.wtune.sql.ast.SqlNode.MySQL;
import static sjtu.ipads.wtune.sql.plan.PlanSupport.assemblePlan;
import static sjtu.ipads.wtune.sql.plan.PlanSupport.translateAsAst;
import static sjtu.ipads.wtune.superopt.runner.RunnerSupport.parseIndices;

public class RunCalciteCases implements Runner {
  private Path testCases;
  private Path rulesFile;
  private App app;
  private boolean verbose;
  private TIntSet targetLines;

  @Override
  public void prepare(String[] argStrings) {
    final Args args = Args.parse(argStrings, 1);
    final String lineRangeSpec = args.getOptional("T", "line", String.class, null);
    testCases = Path.of(args.getOptional("i", "cases", String.class, "wtune_data/calcite_tests"));
    rulesFile = Path.of(args.getOptional("R", "rules", String.class, "wtune_data/rules.txt"));
    verbose = args.getOptional("v", "verbose", boolean.class, false);
    app = App.of("calcite_test");

    if (lineRangeSpec != null) targetLines = parseIndices(lineRangeSpec);

    if (!Files.exists(rulesFile)) throw new IllegalArgumentException("no such file: " + rulesFile);
    if (!Files.exists(testCases)) throw new IllegalArgumentException("no such file: " + testCases);
  }

  @Override
  public void run() throws Exception {
    final List<String> lines = Files.readAllLines(testCases);
    final List<QueryPair> pairs = readPairs(lines);
    System.out.printf("total pairs: %d, supported: %d\n", lines.size() / 2, pairs.size());
    final SubstitutionBank bank = SubstitutionSupport.loadBank(rulesFile);
    for (final QueryPair pair : pairs) {
      if (targetLines != null && !targetLines.contains(pair.lineNum)) continue;
      final Optimizer optimizer = Optimizer.mk(bank);
      optimizer.setTracing(targetLines != null);

      final Set<PlanContext> optimized = optimizer.optimize(pair.p0);
      if (targetLines != null || optimized.size() > 1) {
        System.out.printf("==== optimized of line %d ====\n", pair.lineNum);
        System.out.println("Original Query: ");
        System.out.println("  " + pair.q0);
        System.out.println("SPES result: ");
        System.out.println("  " + pair.q1);
        System.out.println("WeTune result: ");
        for (PlanContext opt : optimized) {
          System.out.println("  " + translateAsAst(opt, opt.root(), false));
          if (verbose && targetLines != null) OptimizerSupport.dumpTrace(optimizer, opt);
        }
      }
    }
  }

  private static class QueryPair {
    private final int lineNum;
    private final SqlNode q0, q1;
    private final PlanContext p0, p1;

    private QueryPair(int lineNum, SqlNode q0, SqlNode q1, PlanContext p0, PlanContext p1) {
      this.lineNum = lineNum;
      this.q0 = q0;
      this.q1 = q1;
      this.p0 = p0;
      this.p1 = p1;
    }
  }

  private List<QueryPair> readPairs(List<String> lines) {
    final Schema schema = app.schema("base");
    SqlSupport.muteParsingError();

    final List<QueryPair> pairs = new ArrayList<>(lines.size() >> 1);
    for (int i = 0, bound = lines.size(); i < bound; i += 2) {
      final String first = lines.get(i), second = lines.get(i + 1);
      final SqlNode q0 = parseSql(MySQL, first);
      final SqlNode q1 = parseSql(MySQL, second);

      if (q0 == null || !PlanSupport.isSupported(q0)) {
        if (verbose) System.err.printf("unsupported query at line %d: %s \n", i + 1, first);
        continue;
      }
      if (q1 == null || !PlanSupport.isSupported(q1)) {
        if (verbose) System.err.printf("unsupported query at line %d: %s \n", i + 2, second);
        continue;
      }

      q0.context().setSchema(schema);
      q1.context().setSchema(schema);
      NormalizationSupport.normalizeAst(q0);
      NormalizationSupport.normalizeAst(q1);

      final PlanContext p0 = assemblePlan(q0, schema);
      if (p0 == null) {
        if (verbose) {
          System.err.printf("wrong query at line %d: %s\n", i + 1, first);
          System.err.println(PlanSupport.getLastError());
        }
        continue;
      }
      final PlanContext p1 = assemblePlan(q1, schema);
      if (p1 == null) {
        if (verbose) {
          System.err.printf("wrong query at line %d: %s\n", i + 2, second);
          System.err.println(PlanSupport.getLastError());
        }
        continue;
      }

      pairs.add(new QueryPair(i + 1, q0, q1, p0, p1));
    }

    return pairs;
  }
}
