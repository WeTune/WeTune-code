package sjtu.ipads.wtune.solver;

import org.apache.commons.lang3.tuple.Triple;
import sjtu.ipads.wtune.solver.core.SolverContext;
import sjtu.ipads.wtune.solver.node.AlgNode;
import sjtu.ipads.wtune.solver.node.JoinType;
import sjtu.ipads.wtune.solver.node.SPJNode;
import sjtu.ipads.wtune.solver.node.TableNode;
import sjtu.ipads.wtune.solver.schema.Schema;

import java.util.List;
import java.util.function.Supplier;

import static sjtu.ipads.wtune.solver.sql.expr.InputRef.ref;

public class RegressionTest {
  private static class TestCase {
    private final int q0, q1;
    private final Schema schema;
    private final boolean expected;

    private TestCase(int q0, int q1, Schema schema, boolean expected) {
      this.q0 = q0;
      this.q1 = q1;
      this.schema = schema;
      this.expected = expected;
    }
  }

  private static final Schema SCM_FK =
      Schema.builder()
          .table(builder -> builder.name("a").column("m", null).column("n", null).uniqueKey("m"))
          .table(builder -> builder.name("b").column("x", null).column("y", null).uniqueKey("x"))
          .foreignKey("a.m", "b.x")
          .build();
  private static final Schema SCM_NO_FK =
      Schema.builder()
          .table(builder -> builder.name("a").column("m", null).column("n", null).uniqueKey("m"))
          .table(builder -> builder.name("b").column("x", null).column("y", null).uniqueKey("x"))
          .build();

  private static TestCase t(int q0, int q1, Schema schema, boolean expected) {
    return new TestCase(q0, q1, schema, expected);
  }

  private static void test() {
    final Supplier<AlgNode>[] qs =
        new Supplier[] {
          RegressionTest::q0,
          RegressionTest::q1,
          RegressionTest::q2,
          RegressionTest::q3,
          RegressionTest::q4
        };
    final List<TestCase> tests =
        List.of(
            t(0, 1, SCM_NO_FK, false),
            t(0, 2, SCM_NO_FK, false),
            t(0, 3, SCM_NO_FK, false),
            t(0, 4, SCM_NO_FK, true),
            t(1, 2, SCM_NO_FK, false),
            t(1, 3, SCM_NO_FK, false),
            t(1, 4, SCM_NO_FK, false),
            t(2, 3, SCM_NO_FK, true),
            t(2, 4, SCM_NO_FK, false),
            t(3, 4, SCM_NO_FK, false),
            t(0, 1, SCM_FK, false),
            t(0, 2, SCM_FK, true),
            t(0, 3, SCM_FK, true),
            t(0, 4, SCM_FK, true),
            t(1, 2, SCM_FK, false),
            t(1, 3, SCM_FK, false),
            t(1, 4, SCM_FK, false),
            t(2, 3, SCM_FK, true),
            t(2, 4, SCM_FK, true),
            t(3, 4, SCM_FK, true));

    //    doTest(SCM_FK, qs[0], qs[4]);
    boolean allSucceed = true;
    for (TestCase t : tests) {
      //
      final Triple<AlgNode, AlgNode, Boolean> result = doTest(t.schema, qs[t.q0], qs[t.q1]);
      if (result.getRight() != t.expected) {
        allSucceed = false;

        System.out.println(
            "failed test: " + (t.schema == SCM_FK ? "fk" : "no_fk") + " " + t.q0 + " " + t.q1);
        System.out.println(result.getLeft());
        System.out.println(result.getMiddle());
        System.out.println("expected " + t.expected + ", actually " + result.getRight());
      }
    }
    if (allSucceed) System.out.println("all passed");
  }

  private static Triple<AlgNode, AlgNode, Boolean> doTest(
      Schema schema, Supplier<AlgNode> f0, Supplier<AlgNode> f1) {
    final AlgNode q0 = f0.get(), q1 = f1.get();

    //    System.out.println(q0);
    //    System.out.println(q1);

    final SolverContext ctx = SolverContext.z3().register(schema);

    final boolean unique = ctx.checkUnique(q0, q1);
    final boolean order = ctx.checkOrder(q0, q1);
    final boolean equivalent = ctx.checkEquivalence(q0, q1);

    //    System.out.println("unique: " + unique + ", order: " + order + ", eq: " + equivalent);
    //    System.out.println("==========");
    return Triple.of(q0, q1, unique && order && equivalent);
  }

  // a LEFT b
  private static AlgNode q0() {
    final TableNode a = TableNode.create(SCM_NO_FK.table("a"));
    final TableNode b = TableNode.create(SCM_NO_FK.table("b"));
    return SPJNode.builder()
        .from(a, null)
        .join(b, null, JoinType.LEFT, ref("a.m"), ref("b.x"))
        .projections(ref("a.m"))
        .build();
  }

  // b LEFT a
  private static AlgNode q1() {
    final TableNode a = TableNode.create(SCM_NO_FK.table("a"));
    final TableNode b = TableNode.create(SCM_NO_FK.table("b"));
    return SPJNode.builder()
        .from(b, null)
        .join(a, null, JoinType.LEFT, ref("a.m"), ref("b.x"))
        .projections(ref("a.m"))
        .build();
  }

  // a INNER b
  private static AlgNode q2() {
    final TableNode a = TableNode.create(SCM_NO_FK.table("a"));
    final TableNode b = TableNode.create(SCM_NO_FK.table("b"));
    return SPJNode.builder()
        .from(a, null)
        .join(b, null, JoinType.INNER, ref("a.m"), ref("b.x"))
        .projections(ref("a.m"))
        .build();
  }

  // b INNER a
  private static AlgNode q3() {
    final TableNode t0 = TableNode.create(SCM_NO_FK.table("a"));
    final TableNode t1 = TableNode.create(SCM_NO_FK.table("b"));
    return SPJNode.builder()
        .from(t1, null)
        .join(t0, null, JoinType.INNER, ref("a.m"), ref("b.x"))
        .projections(ref("b.x"))
        //        .filter(binary(ref("b.x"), Operator.EQ, const_(10)))
        //        .orderBy(ref("b.x"))
        //        .forceUnique(true)
        .build();
  }

  // a
  private static AlgNode q4() {
    final TableNode t1 = TableNode.create(SCM_NO_FK.table("a"));
    return SPJNode.builder()
        .from(t1, null)
        .projections(ref("a.m"))
        //        .filter(binary(ref("a.m"), Operator.EQ, const_(10)))
        //        .orderBy(ref("a.m"))
        .build();
  }

  public static void main(String[] args) {
    test();
  }
}
