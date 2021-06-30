package sjtu.ipads.wtune.prover.normalform;

import static sjtu.ipads.wtune.prover.expr.UExpr.add;
import static sjtu.ipads.wtune.prover.expr.UExpr.eqPred;
import static sjtu.ipads.wtune.prover.expr.UExpr.mul;
import static sjtu.ipads.wtune.prover.expr.UExpr.not;
import static sjtu.ipads.wtune.prover.expr.UExpr.sum;
import static sjtu.ipads.wtune.prover.expr.UExpr.table;

import java.util.List;
import sjtu.ipads.wtune.prover.expr.Tuple;
import sjtu.ipads.wtune.prover.expr.UExpr;

public class EliminatorTest {
  public static void main(String[] args) {
    final Tuple x = Tuple.make("x"), y = Tuple.make("y"), z = Tuple.make("z");
    final UExpr r = table("R", x), s = table("S", y), t = table("T", z);
    final UExpr term0 =
        sum(
            List.of(x, y),
            mul(mul(r.copy(), s.copy()), not(sum(List.of(z), add(t.copy(), eqPred(x, z))))));
    final UExpr term1 = sum(List.of(x, y, z), mul(mul(r.copy(), s.copy()), t.copy()));
    final UExpr term2 = sum(List.of(x, y, z), mul(mul(r.copy(), s.copy()), eqPred(x, z)));
    final UExpr term = add(add(term0, term1), term2);

    final Disjunction nf = Normalization.normalize(term);
    System.out.println(nf);

    final Disjunction eliminated = ExecludedMiddleEliminator.eliminateTautology(nf, true);
    System.out.println(eliminated);
  }
}
