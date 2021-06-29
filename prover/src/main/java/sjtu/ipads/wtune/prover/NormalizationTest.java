package sjtu.ipads.wtune.prover;

import static sjtu.ipads.wtune.prover.expr.UExpr.add;
import static sjtu.ipads.wtune.prover.expr.UExpr.mul;
import static sjtu.ipads.wtune.prover.expr.UExpr.not;
import static sjtu.ipads.wtune.prover.expr.UExpr.squash;
import static sjtu.ipads.wtune.prover.expr.UExpr.table;

import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;
import sjtu.ipads.wtune.prover.normalform.Normalization;

public class NormalizationTest {
  public static void main(String[] args) {
    final Tuple t0 = Tuple.make("t0");
    final Tuple t1 = Tuple.make("t1");
    final Tuple t2 = Tuple.make("t2");
    final Tuple t3 = Tuple.make("t3");
    final Tuple t4 = Tuple.make("t4");

    final UExpr expr =
        not(not(squash(not(add(mul(table("A", t0), table("B", t1)), table("C", t2))))));
    System.out.println(expr);
    System.out.println(Normalization.normalize(expr));
  }
}
