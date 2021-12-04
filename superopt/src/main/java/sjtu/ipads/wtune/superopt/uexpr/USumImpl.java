package sjtu.ipads.wtune.superopt.uexpr;

import java.util.Set;

import static sjtu.ipads.wtune.common.utils.Commons.joining;

record USumImpl(Set<UVar> boundedVars, UTerm body) implements USum {
  @Override
  public UTerm replaceBaseVar(UVar baseVar, UVar repVar) {
    if (boundedVars.contains(baseVar)) return this;
    final UTerm e = body.replaceBaseVar(baseVar, repVar);
    if (e != body) return USum.mk(boundedVars, e);
    else return this;
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder("\u2211");
    joining("{", ",", "}", false, boundedVars, builder);
    builder.append('(').append(body).append(')');
    return builder.toString();
  }
}
