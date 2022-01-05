package sjtu.ipads.wtune.superopt.runner;

import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.sql.plan.PlanContext;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionBank;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.sql.SqlSupport.parseSql;
import static sjtu.ipads.wtune.sql.plan.PlanSupport.assemblePlan;

public class TestCalcite implements Runner {
  private Path inputFile;
  private Path substitutionFile;

  @Override
  public void prepare(String[] argStrings) {
    final Args args = Args.parse(argStrings, 1);
    inputFile = Path.of(args.getOptional("-i", String.class, "wtune_data/calcite_tests"));
    substitutionFile = Path.of(args.getOptional("-s", String.class, "wtune_data/substitutions"));
  }

  private static class QueryPair {
    private int lineNum;
    private final SqlNode q0, q1;
    private PlanContext p0, p1;

    private QueryPair(SqlNode q0, SqlNode q1) {
      this.q0 = q0;
      this.q1 = q1;
    }
  }

  private List<QueryPair> mkPairs(List<String> lines) {
    final List<QueryPair> pairs = new ArrayList<>(lines.size() >> 1);
    int lineNum = -1, supported = 0, unsupported = 0, wrong = 0;
    String tmp = null;
    for (String line : lines) {
      ++lineNum;
      //      System.err.println(lineNum);
      if ((lineNum & 1) == 0) {
        tmp = line;
        continue;
      }

      assert tmp != null;
      final Statement s0 = Statement.mk("calcite_test", tmp, null);
      final Statement s1 = Statement.mk("calcite_test", line, null);
      if (s0.ast() == null || s1.ast() == null) {
        ++unsupported;
        continue;
      }

      final QueryPair pair =
          new QueryPair(
              parseSql(s0.app().dbType(), s0.rawSql()), parseSql(s1.app().dbType(), s1.rawSql()));

      try {
        pair.lineNum = lineNum;
        pair.p0 = assemblePlan(pair.q0, s0.app().schema("base"));
        pair.p1 = assemblePlan(pair.q1, s1.app().schema("base"));
        ++supported;
        pairs.add(pair);

      } catch (IllegalArgumentException ex) {
        if (ex.getMessage().startsWith("failed to bind reference")) ++wrong;
        else ++unsupported;
      }
    }

    System.out.printf("supported: %d unsupported: %d wrong: %d\n", supported, unsupported, wrong);
    return pairs;
  }

  @Override
  public void run() throws Exception {
    final List<QueryPair> pairs = mkPairs(Files.readAllLines(inputFile));
    final SubstitutionBank bank = SubstitutionSupport.loadBank(substitutionFile);
    int index = -1;
    for (QueryPair pair : pairs) {
      ++index;
      // TODO
      //      final Set<PlanNode> optimized = OptimizerSupport.optimize(bank, pair.p0);
      final Set<PlanContext> optimized = null;
      System.out.printf("%d. %s\n", pair.lineNum, optimized);
      //      System.out.printf("%d: %d\n", index, optimized.size());
    }
  }
}
