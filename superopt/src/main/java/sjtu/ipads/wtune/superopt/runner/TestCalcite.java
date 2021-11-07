package sjtu.ipads.wtune.superopt.runner;

import sjtu.ipads.wtune.sqlparser.ASTParser;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanSupport;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.superopt.optimizer.OptimizerSupport;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionBank;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    private final ASTNode q0, q1;
    private PlanNode p0, p1;

    private QueryPair(ASTNode q0, ASTNode q1) {
      this.q0 = q0;
      this.q1 = q1;
    }
  }

  private List<QueryPair> mkPairs(List<String> lines) {
    ASTParser.setErrorMuted(true);

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
      if (s0.parsed() == null || s1.parsed() == null) {
        ++unsupported;
        continue;
      }

      final QueryPair pair = new QueryPair(s0.parsed(), s1.parsed());
      try {
        pair.lineNum = lineNum;
        pair.p0 = PlanSupport.assemblePlan(pair.q0, s0.app().schema("base"));
        pair.p1 = PlanSupport.assemblePlan(pair.q1, s1.app().schema("base"));
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
      final Set<PlanNode> optimized = OptimizerSupport.optimize(bank, pair.p0);
      System.out.printf("%d. %s\n", pair.lineNum, optimized);
      //      System.out.printf("%d: %d\n", index, optimized.size());
    }
  }
}
