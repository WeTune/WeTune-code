package sjtu.ipads.wtune.superopt;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sql.ast.ExprKind;
import sjtu.ipads.wtune.sql.ast.SqlContext;
import sjtu.ipads.wtune.sql.ast.SqlKind;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.sql.plan.PlanContext;
import sjtu.ipads.wtune.sql.plan.PlanSupport;
import sjtu.ipads.wtune.sql.schema.Schema;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.superopt.optimizer.Optimizer;
import sjtu.ipads.wtune.superopt.substitution.Substitution;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionBank;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static sjtu.ipads.wtune.sql.SqlSupport.parseSql;
import static sjtu.ipads.wtune.sql.ast.SqlKind.GroupItem;
import static sjtu.ipads.wtune.sql.ast.SqlNode.MySQL;
import static sjtu.ipads.wtune.sql.plan.PlanSupport.assemblePlan;
import static sjtu.ipads.wtune.sql.plan.PlanSupport.stringifyTree;
import static sjtu.ipads.wtune.sql.support.action.NormalizationSupport.normalizeAst;
import static sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport.loadBank;

public class MiscTest {
  @Test
  void testOrdinal() {
    int total = 30;
    int ordinal = 0;
    for (int i = 0; i < total; ++i) {
      for (int j = i; j < total; ++j) {
        assertEquals(ordinal, ordinal(total, i, j), i + "," + j);
        ++ordinal;
      }
    }
  }

  private int ordinal(int total, int i, int j) {
    assert i <= j;
    return ((total * 2) - i + 1) * i / 2 + j - i;
  }

  @Test
  void test() throws IOException {
    final App calcite = App.of("calcite_test");
    final String sql =
        "SELECT EMP1.EMPNO, EMP1.ENAME, EMP1.JOB, EMP1.MGR, EMP1.HIREDATE, EMP1.SAL, EMP1.COMM, EMP1.DEPTNO, EMP1.SLACKER FROM EMP AS EMP1 INNER JOIN ( SELECT EMP2.DEPTNO FROM EMP AS EMP2 GROUP BY EMP2.DEPTNO ) AS t4 ON EMP1.DEPTNO = t4.DEPTNO";
    final SqlNode ast = parseSql(MySQL, sql);
    final Schema schema = calcite.schema("base");
    ast.context().setSchema(schema);
    normalizeAst(ast);
    final PlanContext plan = assemblePlan(ast, schema);
    final SubstitutionBank bank = loadBank(Path.of("wtune_data", "rules.test.txt"));
    final Optimizer optimizer = Optimizer.mk(bank);
    final Set<PlanContext> optimized = optimizer.optimize(plan);
    for (PlanContext opt : optimized) {
      System.out.println(opt);
      //      System.out.println(PlanSupport.translateAsAst(opt, opt.root(),
      // false).toString(false));
    }
  }

  @Test
  void test1() throws IOException {
    final SubstitutionBank bank0 = loadBank(Path.of("wtune_data", "rules", "rules.2.txt"));
    final SubstitutionBank bank1 = loadBank(Path.of("wtune_data", "rules", "rules.test.txt"));
    int count = 0;
    for (Substitution rule : bank1.rules()) {
      if (!bank0.contains(rule)) {
        System.out.println(rule);
        ++count;
      }
    }
    System.out.println(bank1.size() + " " + count);
  }
}
