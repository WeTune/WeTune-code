package sjtu.ipads.wtune.superopt.runner;

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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static sjtu.ipads.wtune.common.utils.IOSupport.checkFileExists;
import static sjtu.ipads.wtune.sql.SqlSupport.parseSql;
import static sjtu.ipads.wtune.sql.ast.SqlNode.MySQL;
import static sjtu.ipads.wtune.sql.plan.PlanSupport.*;
import static sjtu.ipads.wtune.superopt.optimizer.OptimizerSupport.*;
import static sjtu.ipads.wtune.superopt.runner.RunnerSupport.dataDir;

public class RunCalciteTest implements Runner {
  private String target;
  private Path testCasesPath, rulesPath;
  private App app;
  private int verbosity;

  private interface Task {
    void execute(RunCalciteTest runner) throws Exception;
  }

  private static final Map<String, Task> TASKS =
      Map.of(
          "CompareVerifyRule", RunCalciteTest::compareVerifyingRule,
          "CompareVerifyQuery", RunCalciteTest::compareVerifyingQuery);

  @Override
  public void prepare(String[] argStrings) throws Exception {
    final Args args = Args.parse(argStrings, 1);

    app = App.of("calcite_test");
    target = args.getOptional("T", "task", String.class, "all");
    verbosity = args.getOptional("v", "verbose", int.class, 0);

    if (!target.equals("all") && !TASKS.containsKey(target))
      throw new IllegalArgumentException("no such task: " + target);

    final Path dataDir = dataDir();
    final String testCasesFile = args.getOptional("i", "input", String.class, "calcite_tests");
    final String rulesFile = args.getOptional("R", "rules", String.class, "rules.txt");
    testCasesPath = dataDir.resolve(testCasesFile);
    rulesPath = dataDir.resolve(rulesFile);
  }

  @Override
  public void run() throws Exception {
    if (target.equals("all")) {
      for (var pair : TASKS.entrySet()) {
        System.out.println("Begin: " + pair.getKey());
        pair.getValue().execute(this);
      }
    } else {
      TASKS.get(target).execute(this);
    }
  }

  private void compareVerifyingRule() throws IOException {
    checkFileExists(testCasesPath);
    checkFileExists(rulesPath);
    OptimizerSupport.setOptimizerTweaks(
        TWEAK_KEEP_ORIGINAL_PLAN | TWEAK_SORT_FILTERS | TWEAK_PERMUTE_JOIN_TREE);

    final List<QueryPair> pairs = readPairs(Files.readAllLines(testCasesPath));
    final SubstitutionBank rules = SubstitutionSupport.loadBank(rulesPath);

    int count = 0;
    outer:
    for (final QueryPair pair : pairs) {
      final Optimizer opt0 = Optimizer.mk(rules);
      final Optimizer opt1 = Optimizer.mk(rules);
      //      opt0.setTracing(true);
      //      opt1.setTracing(true);
      final Set<PlanContext> rewritten0 = opt0.optimize(pair.p0);
      final Set<PlanContext> rewritten1 = opt1.optimize(pair.p1);

      for (PlanContext plan0 : rewritten0)
        for (PlanContext plan1 : rewritten1)
          if (isLiteralEq(plan0, plan1)) {
            //            OptimizerSupport.dumpTrace(opt0, plan0);
            //            OptimizerSupport.dumpTrace(opt1, plan1);
            ++count;
            System.out.printf("%d,%d\n", pair.lineNum, pair.lineNum + 1);
            if (verbosity >= 1) {
              System.out.println("Both are rewritten to: ");
              System.out.println(translateAsAst(plan0, plan0.root(), false).toString());
            }
            continue outer;
          }
    }

    System.out.println("Total: " + count);
  }

  private void compareVerifyingQuery() {}

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

    private int q0Id() {
      return lineNum;
    }

    private int q1Id() {
      return lineNum + 1;
    }

    private int pairId() {
      return lineNum + 1 >> 1;
    }
  }

  private List<QueryPair> readPairs(List<String> lines) {
    final Schema schema = app.schema("base");
    SqlSupport.muteParsingError();

    final List<QueryPair> pairs = new ArrayList<>(lines.size() >> 1);
    for (int i = 0, bound = lines.size(); i < bound; i += 2) {
      //      if (i != 246) continue;
      final String first = lines.get(i), second = lines.get(i + 1);
      final SqlNode q0 = parseSql(MySQL, first);
      final SqlNode q1 = parseSql(MySQL, second);

      if (q0 == null || !PlanSupport.isSupported(q0)) {
        if (verbosity >= 2) System.err.printf("unsupported query at line %d: %s \n", i + 1, first);
        continue;
      }
      if (q1 == null || !PlanSupport.isSupported(q1)) {
        if (verbosity >= 2) System.err.printf("unsupported query at line %d: %s \n", i + 2, second);
        continue;
      }

      q0.context().setSchema(schema);
      q1.context().setSchema(schema);
      NormalizationSupport.normalizeAst(q0);
      NormalizationSupport.normalizeAst(q1);

      final PlanContext p0 = assemblePlan(q0, schema);
      if (p0 == null) {
        if (verbosity >= 2) {
          System.err.printf("wrong query at line %d: %s\n", i + 1, first);
          System.err.println(PlanSupport.getLastError());
        }
        continue;
      }
      final PlanContext p1 = assemblePlan(q1, schema);
      if (p1 == null) {
        if (verbosity >= 2) {
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
