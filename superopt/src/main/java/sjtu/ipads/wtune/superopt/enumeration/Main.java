package sjtu.ipads.wtune.superopt.enumeration;

import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.fragment1.Fragment;
import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.util.List;

import static sjtu.ipads.wtune.prover.ProverSupport.mkLogicCtx;
import static sjtu.ipads.wtune.superopt.enumeration.EnumerationSupport.mkEnumTree;

public class Main {
  public static void main(String[] args) {
    final Fragment f0 = Fragment.parse("Proj*(InSubFilter(Input,Proj(Input)))", null);
    final Fragment f1 = Fragment.parse("Proj*(InnerJoin(Input,Input))", null);

    final EnumerationTree tree = mkEnumTree(f0, f1, mkLogicCtx());
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
      System.out.println(Substitution.mk(f0, f1, constraints));
    }
  }
}
