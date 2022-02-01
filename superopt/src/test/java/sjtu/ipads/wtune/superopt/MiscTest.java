package sjtu.ipads.wtune.superopt;

import org.apache.commons.lang3.tuple.Pair;
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
import sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport;

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
    final SubstitutionBank bank = loadBank(Path.of("wtune_data", "rules", "rules.raw.txt"));
    final String ruleStr =
        "InSubFilter<a4>(InnerJoin<a1 a2>(Proj*<a0 s0>(Input<t0>),Input<t1>),Proj<a3 s1>(Input<t2>))|InnerJoin<a8 a9>(Proj<a7 s2>(InnerJoin<a5 a6>(Input<t3>,Input<t4>)),Input<t5>)|AttrsSub(a0,t0);AttrsSub(a1,s0);AttrsSub(a2,t1);AttrsSub(a3,t2);AttrsSub(a4,s0);Unique(t0,a0);Unique(t2,a3);TableEq(t3,t2);TableEq(t4,t0);TableEq(t5,t1);AttrsEq(a5,a3);AttrsEq(a6,a4);AttrsEq(a7,a0);AttrsEq(a8,a1);AttrsEq(a9,a2);SchemaEq(s2,s0)";
    final Substitution rule = Substitution.parse(ruleStr);
    final Pair<PlanContext, PlanContext> plan = SubstitutionSupport.translateAsPlan(rule);
    completePlan(plan.getLeft());
    Optimizer.mk(bank).optimize(plan.getLeft());
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
