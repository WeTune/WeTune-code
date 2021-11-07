package sjtu.ipads.wtune.prover.uexpr2;

import static java.util.Arrays.asList;
import static sjtu.ipads.wtune.common.utils.Commons.joining;

record USumImpl(UVar[] sumVars, UExpr body) implements USum {
  @Override
  public UExpr replaceBaseVar(UVar baseVar, UVar repVar) {
    if (asList(sumVars).contains(baseVar)) return this;
    final UExpr e = body.replaceBaseVar(baseVar, repVar);
    if (e != body) return USum.mk(sumVars, e);
    else return this;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder("\u2211");
    joining("{", ",", "}", false, asList(sumVars), builder);
    builder.append('(').append(body).append(')');
    return builder.toString();
  }
}
