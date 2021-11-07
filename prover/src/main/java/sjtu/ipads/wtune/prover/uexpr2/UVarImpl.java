package sjtu.ipads.wtune.prover.uexpr2;

import java.util.Arrays;

import static java.util.Arrays.asList;
import static sjtu.ipads.wtune.common.utils.Commons.joining;

record UVarImpl(VarKind kind, UName name, UVar[] usedVars) implements UVar {
  @Override public UVar replaceBaseVar(UVar baseVar, UVar repVar) {
    if (baseVar.kind() != VarKind.BASE) throw new IllegalArgumentException();
    if (this.kind == VarKind.BASE && this.equals(baseVar)) return repVar;

    UVar[] vars = usedVars;
    for (int i = 0; i < vars.length; i++) {
      final UVar var = vars[i];
      final UVar v = vars[i].replaceBaseVar(baseVar, repVar);
      if (v != var) {
        if (vars == usedVars) vars = Arrays.copyOf(usedVars, usedVars.length);
        vars[i] = v;
      }
    }

    if (vars != usedVars) return new UVarImpl(kind, name, vars);
    else return this;
  }

  @Override
  public String toString() {
    if (kind == VarKind.BASE) return name.toString();
    if (kind == VarKind.EQ) return usedVars[0] + " = " + usedVars[1];

    final StringBuilder builder = new StringBuilder(name.toString());
    return joining("(", ",", ")", false, asList(usedVars), builder).toString();
  }
}
