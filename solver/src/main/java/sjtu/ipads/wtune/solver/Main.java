package sjtu.ipads.wtune.solver;

import sjtu.ipads.wtune.solver.core.SolverContext;
import sjtu.ipads.wtune.solver.node.AlgNode;
import sjtu.ipads.wtune.solver.node.JoinType;
import sjtu.ipads.wtune.solver.node.SPJNode;
import sjtu.ipads.wtune.solver.node.TableNode;
import sjtu.ipads.wtune.solver.schema.Schema;
import sjtu.ipads.wtune.solver.sql.Operator;

import java.util.function.Supplier;

import static sjtu.ipads.wtune.solver.sql.expr.Expr.binary;
import static sjtu.ipads.wtune.solver.sql.expr.Expr.subquery;
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
    doTest(Main::q0, Main::q1);
  }

  private static void doTest(Supplier<AlgNode> f0, Supplier<AlgNode> f1) {
    final AlgNode q0 = f0.get(), q1 = f1.get();

    System.out.println("==========");
    System.out.println(q0);
    System.out.println(q1);

    final SolverContext ctx = SolverContext.z3().register(schema);

    final boolean unique = ctx.checkUnique(q0, q1);
    final boolean order = ctx.checkOrder(q0, q1);
    final boolean equivalent = ctx.checkEquivalence(q0, q1);

    System.out.println("unique: " + unique + ", order: " + order + ", eq: " + equivalent);
  }

  // a LEFT b
  private static AlgNode q0() {
    final TableNode a = TableNode.create(schema.table("a"));
    final TableNode b = TableNode.create(schema.table("b"));
    return SPJNode.builder()
        .from(a, null)
        .join(b, null, JoinType.INNER, ref("a.m"), ref("b.x"))
        .projections(ref("b.x"))
        .build();
  }

  // b LEFT a
  private static AlgNode q1() {
    final TableNode a = TableNode.create(schema.table("a"));
    final TableNode b = TableNode.create(schema.table("b"));
    final SPJNode sub =
        SPJNode.builder().from(b, null).projections(ref("b.x")).orderBy(ref("b.x")).build();

    return SPJNode.builder()
        .from(a, null)
        .filter(binary(ref("a.m"), Operator.IN_SUB, subquery(sub)))
        .projections(ref("a.m"))
        .build();
  }
}
