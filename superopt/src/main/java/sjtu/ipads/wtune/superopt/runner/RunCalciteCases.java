package sjtu.ipads.wtune.superopt.runner;

import sjtu.ipads.wtune.sql.SqlSupport;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.sql.plan.PlanContext;
import sjtu.ipads.wtune.sql.plan.PlanSupport;
import sjtu.ipads.wtune.sql.schema.Schema;
import sjtu.ipads.wtune.sql.support.action.NormalizationSupport;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.superopt.optimizer.Optimizer;
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

public class RunCalciteCases implements Runner {
  private Path testCasesFile;
  private Path rulesFile;
  private App app;
  private boolean echo;

  @Override
  public void prepare(String[] argStrings) {
    final Args args = Args.parse(argStrings, 1);
    testCasesFile = Path.of(args.getOptional("-i", String.class, "wtune_data/calcite_tests"));
    rulesFile = Path.of(args.getOptional("-s", String.class, "wtune_data/rules.txt"));
    echo = args.getOptional("-e", boolean.class, false);
    app = App.of("calcite_test");

    if (!Files.exists(rulesFile)) throw new IllegalArgumentException("no such file: " + rulesFile);
    if (!Files.exists(testCasesFile))
      throw new IllegalArgumentException("no such file: " + testCasesFile);
  }

  @Override
  public void run() throws Exception {
    final List<String> lines = Files.readAllLines(testCasesFile);
    final List<QueryPair> pairs = readPairs(lines);
    System.out.printf("total pairs: %d, supported: %d\n", lines.size() / 2, pairs.size());
    final SubstitutionBank bank = SubstitutionSupport.loadBank(rulesFile);
    for (int i = 0, bound = pairs.size(); i < bound; ++i) {
      final QueryPair pair = pairs.get(i);
      final Optimizer optimizer = Optimizer.mk(bank);
      final Set<PlanContext> optimized = optimizer.optimize(pair.p0);
      if (optimized.size() > 1) {
        System.out.printf("==== optimized of line %d ====\n", pair.lineNum);
        System.out.println("SPES result: ");
        System.out.println("  " + pair.q1);
        System.out.println("WeTune result: ");
        for (PlanContext opt : optimized) {
          System.out.println("  " + translateAsAst(opt, opt.root(), false));
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
        if (echo) System.err.printf("unsupported query at line %d: %s \n", i + 1, first);
        continue;
      }
      if (q1 == null || !PlanSupport.isSupported(q1)) {
        if (echo) System.err.printf("unsupported query at line %d: %s \n", i + 2, second);
        continue;
      }

      q0.context().setSchema(schema);
      q1.context().setSchema(schema);
      NormalizationSupport.normalizeAst(q0);
      NormalizationSupport.normalizeAst(q1);

      final PlanContext p0 = assemblePlan(q0, schema);
      if (p0 == null) {
        if (echo) {
          System.err.printf("wrong query at line %d: %s\n", i + 1, first);
          System.err.println(PlanSupport.getLastError());
        }
        continue;
      }
      final PlanContext p1 = assemblePlan(q1, schema);
      if (p1 == null) {
        if (echo) {
          System.err.printf("wrong query at line %d: %s\n", i + 2, second);
          System.err.println(PlanSupport.getLastError());
        }
        continue;
      }

      pairs.add(new QueryPair(i, q0, q1, p0, p1));
    }

    return pairs;
  }
}
