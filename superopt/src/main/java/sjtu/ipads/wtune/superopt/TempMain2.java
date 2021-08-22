package sjtu.ipads.wtune.superopt;

import sjtu.ipads.wtune.superopt.substitution.Substitution;

import static sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport.printReadable;

public class TempMain2 {
  public static void main(String[] args) {
    final String str =
        "Filter<p0 a0>(Proj*<a1>(InnerJoin<a2 a3>(Input<t0>,Input<t1>)))|Proj<a4>(Filter<p1 a5>(InnerJoin<a6 a7>(Input<t2>,Input<t3>)))|TableEq(t0,t2);TableEq(t1,t3);AttrsEq(a1,a3);AttrsEq(a1,a7);AttrsEq(a2,a4);AttrsEq(a2,a5);AttrsEq(a2,a6);AttrsEq(a3,a7);AttrsEq(a4,a5);AttrsEq(a4,a6);AttrsEq(a5,a6);PredicateEq(p0,p1);AttrsSub(a0,a1);AttrsSub(a1,t1);AttrsSub(a2,t0);AttrsSub(a3,t1);AttrsSub(a4,t2);AttrsSub(a5,t2);AttrsSub(a6,t2);AttrsSub(a7,t3);Unique(t0,a2);Unique(t1,a1);Unique(t1,a3);Unique(t2,a4);Unique(t2,a5);Unique(t2,a6);Unique(t3,a7)";
    final Substitution substitution = Substitution.parse(str);
    printReadable(substitution);
  }
}
