package sjtu.ipads.wtune.superopt;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.sql.plan.*;
import sjtu.ipads.wtune.sql.schema.Schema;
import sjtu.ipads.wtune.sql.support.action.NormalizationSupport;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.superopt.optimizer.Optimizer;
import sjtu.ipads.wtune.superopt.substitution.Substitution;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionBank;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.common.utils.IterableSupport.zip;
import static sjtu.ipads.wtune.sql.SqlSupport.parseSql;
import static sjtu.ipads.wtune.sql.ast.SqlNode.MySQL;
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
    NormalizationSupport.normalizeAst(ast);
    final PlanContext plan = PlanSupport.assemblePlan(ast, schema);
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

  private static void completePlan(PlanContext plan) {
    final int oldRoot = plan.root();
    final PlanKind oldRootKind = plan.kindOf(oldRoot);
    if (oldRootKind != PlanKind.Join && !oldRootKind.isFilter()) return;

    final ValuesRegistry valuesReg = plan.valuesReg();
    final Values inValues = valuesReg.valuesOf(oldRoot);
    final List<String> names = ListSupport.map(inValues, Value::name);
    final List<Expression> exprs = ListSupport.map(inValues, PlanSupport::mkColRefExpr);
    zip(exprs, inValues, (e, v) -> valuesReg.bindValueRefs(e, newArrayList(v)));

    final ProjNode proj = ProjNode.mk(false, names, exprs);
    final int projNode = plan.bindNode(proj);
    plan.setChild(projNode, 0, oldRoot);
    plan.setRoot(projNode);
  }
}
