package sjtu.ipads.wtune.superopt.enumeration;

import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.util.List;

import static sjtu.ipads.wtune.prover.ProverSupport.mkLogicCtx;
import static sjtu.ipads.wtune.superopt.enumeration.EnumerationSupport.mkTree;

public class Main {
  public static void main(String[] args) {
    final String str =
        "Proj<c0>(InnerJoin<c1 c2>(Input<t0>,Input<t1>))|Proj<c3>(Input<t2>)|TableEq(t0,t2);PickEq(c0,c2);PickEq(c1,c3);Reference(t0,c1,t1,c2);"
            + "NotNull(t0,c1);Unique(t1,c2);"
            + "AttrsSub(c0,t1);AttrsSub(c1,t0);AttrsSub(c2,t1);AttrsSub(c3,t2)";
    final Substitution sub = Substitution.parse(str);

    final EnumerationTree tree = mkTree(sub._0(), sub._1(), mkLogicCtx());
    //    System.out.println(
    //        tree.prove(
    //            new boolean[] {
    //              true, false, false, false, false, false, false, false, false, false, false,
    // false,
    //              false, false, false, false, false, false, true, true, true, false, true, true,
    // true,
    //              true, false, true, true, true, true, false, true, true
    //            }));
    tree.enumerate();
    for (List<Constraint> constraints : tree.results()) {
      System.out.println(Substitution.mk(sub._0(), sub._1(), constraints));
    }
  }
}
