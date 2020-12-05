package sjtu.ipads.wtune.solver;

import sjtu.ipads.wtune.solver.core.SolverContext;
import sjtu.ipads.wtune.solver.node.AlgNode;
import sjtu.ipads.wtune.solver.node.JoinType;
import sjtu.ipads.wtune.solver.node.SPJNode;
import sjtu.ipads.wtune.solver.node.TableNode;
import sjtu.ipads.wtune.solver.schema.Schema;
import sjtu.ipads.wtune.solver.sql.Operator;

import java.util.function.Supplier;

import static sjtu.ipads.wtune.solver.node.UnionNode.union;
import static sjtu.ipads.wtune.solver.sql.expr.Expr.binary;
import static sjtu.ipads.wtune.solver.sql.expr.Expr.const_;
import static sjtu.ipads.wtune.solver.sql.expr.InputRef.ref;

public class Main {
  private static final Schema schema =
      Schema.builder()
          .table(builder -> builder.name("a").column("m", null).column("n", null))
          .table(builder -> builder.name("b").column("x", null).column("y", null))
          .foreignKey("a.m", "b.x")
          .build();

  public static void main(String[] args) {
    test();
  }

  private static void test() {
    doTest(Main::q1, Main::q0);
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

  private static AlgNode q0() {
    final TableNode a = TableNode.create(schema.table("a"));
    //    final TableNode b = TableNode.create(schema.table("b"));
    return SPJNode.builder()
        .from(a, null)
        .filter(
            binary(
                binary(ref("a.n"), Operator.EQ, const_(2)),
                Operator.OR,
                binary(ref("a.n"), Operator.EQ, const_(1))))
        .projections(ref("a.m"))
        .forceUnique(true)
        .build();
  }

  private static AlgNode q1() {
    final TableNode a = TableNode.create(schema.table("a"));
    //    final TableNode b = TableNode.create(schema.table("b"));
    final SPJNode q0 =
        SPJNode.builder()
            .from(a, null)
            .filter(binary(ref("a.n"), Operator.EQ, const_(2)))
            .projections(ref("a.m"))
            .build();
    final SPJNode q1 =
        SPJNode.builder()
            .from(a, null)
            .filter(binary(ref("a.n"), Operator.EQ, const_(1)))
            .projections(ref("a.m"))
            .build();

    return union(q0, q1);
  }
}
