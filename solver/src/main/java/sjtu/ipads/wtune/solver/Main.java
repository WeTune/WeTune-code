package sjtu.ipads.wtune.solver;

import sjtu.ipads.wtune.solver.core.SolverContext;
import sjtu.ipads.wtune.solver.node.AlgNode;
import sjtu.ipads.wtune.solver.node.JoinType;
import sjtu.ipads.wtune.solver.node.SPJNode;
import sjtu.ipads.wtune.solver.node.TableNode;
import sjtu.ipads.wtune.solver.schema.Schema;

import java.util.function.Supplier;

import static sjtu.ipads.wtune.solver.sql.expr.InputRef.ref;

public class Main {
  private static final Schema schema =
      Schema.builder()
          .table(builder -> builder.name("a").column("m", null).column("n", null).uniqueKey("m"))
          .table(builder -> builder.name("b").column("x", null).column("y", null).uniqueKey("x"))
          .foreignKey("a.m", "b.x")
          .build();

  public static void main(String[] args) {
    test();
  }

  private static void test() {
    final Supplier<AlgNode>[] qs =
        new Supplier[] {Main::q0, Main::q1, Main::q2, Main::q3, Main::q4};
    doTest(qs[3], qs[4]);
    //    for (int i = 0; i < qs.length; i++)
    //      for (int j = i + 1; j < qs.length; j++) doTest(qs[i], qs[j]);
  }

  private static void doTest(Supplier<AlgNode> f0, Supplier<AlgNode> f1) {
    final AlgNode q0 = f0.get(), q1 = f1.get();

    System.out.println("==========");
    System.out.println(q0);
    System.out.println(q1);

    final SolverContext ctx = SolverContext.z3();
    final boolean unique = ctx.checkUnique(schema, q0, q1);
    final boolean equivalent = ctx.checkEquivalence(schema, q0, q1);
    System.out.println("unique: " + unique + ", eq: " + equivalent);
  }

  // a LEFT b
  private static AlgNode q0() {
    final TableNode a = TableNode.create(schema.table("a"));
    final TableNode b = TableNode.create(schema.table("b"));
    return SPJNode.builder()
        .from(a, null)
        .join(b, null, JoinType.LEFT, ref("a.m"), ref("b.x"))
        .projections(ref("a.m"))
        .build();
  }

  // b LEFT a
  private static AlgNode q1() {
    final TableNode a = TableNode.create(schema.table("a"));
    final TableNode b = TableNode.create(schema.table("b"));
    return SPJNode.builder()
        .from(b, null)
        .join(a, null, JoinType.LEFT, ref("a.m"), ref("b.x"))
        .projections(ref("a.m"))
        .build();
  }

  // a INNER b
  private static AlgNode q2() {
    final TableNode a = TableNode.create(schema.table("a"));
    final TableNode b = TableNode.create(schema.table("b"));
    return SPJNode.builder()
        .from(a, null)
        .join(b, null, JoinType.INNER, ref("a.m"), ref("b.x"))
        .projections(ref("a.m"))
        .build();
  }

  // b INNER a
  private static AlgNode q3() {
    final TableNode t0 = TableNode.create(schema.table("a"));
    final TableNode t1 = TableNode.create(schema.table("b"));
    return SPJNode.builder()
        .from(t1, null)
        .join(t0, null, JoinType.INNER, ref("a.m"), ref("b.x"))
        .projections(ref("b.x"))
//        .forceUnique(true)
        .build();
  }

  // a
  private static AlgNode q4() {
    final TableNode t1 = TableNode.create(schema.table("a"));
    return SPJNode.builder().from(t1, null).projections(ref("a.m")).build();
  }
}
