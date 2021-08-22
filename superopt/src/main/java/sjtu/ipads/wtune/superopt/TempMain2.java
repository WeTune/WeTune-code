package sjtu.ipads.wtune.superopt;

import sjtu.ipads.wtune.superopt.substitution.Substitution;

import static sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport.printReadable;

public class TempMain2 {
  public static void main(String[] args) {
    final String str =
        "Proj*<a0>(Proj<a1>(Input<t0>))|Proj*<a2>(Input<t1>)|TableEq(t0,t1);AttrsEq(a1,a2);AttrsSub(a0,a1);AttrsSub(a1,t0);AttrsSub(a2,t1)";
    final Substitution substitution = Substitution.parse(str);
    printReadable(substitution);
  }
}
