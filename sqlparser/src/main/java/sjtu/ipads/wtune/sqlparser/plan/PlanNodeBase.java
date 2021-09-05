package sjtu.ipads.wtune.sqlparser.plan;

import gnu.trove.list.TIntList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.Commons.joining;
import static sjtu.ipads.wtune.common.utils.Commons.listJoin;
import static sjtu.ipads.wtune.common.utils.FuncUtils.zipForEach;
import static sjtu.ipads.wtune.sqlparser.plan.PlanSupport.bindValuesRelaxed;

abstract class PlanNodeBase implements PlanNode {
  protected PlanContext context;
  protected PlanNode successor;
  protected final PlanNode[] predecessors;
  private String stringifyCache0, stringifyCache1;

  protected PlanNodeBase() {
    predecessors = new PlanNode[kind().numPredecessors()];
  }

  protected PlanNodeBase(OperatorType type) {
    predecessors = new PlanNode[type.numPredecessors()];
  }

  @Override
  public PlanContext context() {
    return context;
  }

  @Override
  public PlanNode successor() {
    return successor;
  }

  @Override
  public PlanNode[] predecessors() {
    return predecessors;
  }

  @Override
  public void setContext(PlanContext context) {
    if (this.context != null) throw new IllegalStateException("context is immutable once set");

    this.context = context;
  }

  @Override
  public void setSuccessor(PlanNode successor) {
    if (this.successor != null) throw new IllegalStateException("successor is immutable once set");

    this.successor = requireNonNull(successor);
  }

  @Override
  public void setPredecessor(int idx, PlanNode predecessor) {
    Objects.checkIndex(idx, predecessors.length);

    if (predecessors[idx] != null)
      throw new IllegalStateException("predecessor is immutable once set");

    predecessors[idx] = requireNonNull(predecessor);
    predecessor.setSuccessor(this);
  }

  @Override
  public boolean rebindRefs(PlanContext refCtx) {
    rebindRefs(refCtx, refs(), predecessors[0]);
    return true;
  }

  @Override
  public void freeze() {
    for (PlanNode predecessor : predecessors) predecessor.freeze();
    stringifyCache0 = stringify0(new StringBuilder(), false).toString();
    stringifyCache1 = stringify0(new StringBuilder(), true).toString();
  }

  @Override
  public String toString() {
    return toString(false);
  }

  @Override
  public String toString(boolean compact) {
    if (!compact && stringifyCache0 != null) return stringifyCache0;
    if (compact && stringifyCache1 != null) return stringifyCache1;
    return stringify0(new StringBuilder(), compact).toString();
  }

  @Override
  public StringBuilder stringifyCompact(StringBuilder builder) {
    if (stringifyCache1 != null) return builder.append(stringifyCache1);
    else return stringify0(builder, true);
  }

  @Override
  public StringBuilder stringify(StringBuilder builder) {
    if (stringifyCache0 != null) return builder.append(stringifyCache0);
    else return stringify0(builder, false);
  }

  protected void checkContextSet() {
    if (context == null) throw new IllegalStateException("unresolved plan");
  }

  protected void rebindRefs(PlanContext refCtx, List<Ref> refs, PlanNode predecessor) {
    final List<Value> oldValues = refCtx.deRef(refs);
    final List<Value> newValues = bindValuesRelaxed(oldValues, refCtx, predecessor);
    if (oldValues != newValues) zipForEach(refs, newValues, context::setRef);
  }

  protected boolean rebindRefs(
      PlanContext refCtx, List<Ref> refs, TIntList hints, PlanNode input0, PlanNode input1) {
    final List<Value> oldValues = refCtx.deRef(refs);

    final List<Value> newValues0 = bindValuesRelaxed(oldValues, refCtx, input0, true);
    if (newValues0 == oldValues) return true;

    final List<Value> newValues1 = bindValuesRelaxed(oldValues, refCtx, input1, true);
    if (newValues1 == oldValues) return true;

    final ValueBag inValues = input1.values();

    final PlanContext ctx = context();
    final List<Value> newValues = new ArrayList<>(oldValues.size());
    for (int i = 0, bound = oldValues.size(); i < bound; i++) {
      if (values().contains(oldValues.get(i))) newValues.add(oldValues.get(i));
      else if (newValues0.get(i) != null) newValues.add(newValues0.get(i));
      else if (newValues1.get(i) != null) newValues.add(newValues1.get(i));
      else if (hints.get(i) != -1) {
        final Value refValue = inValues.get(hints.get(i));
        final Value usedValue =
            refValue.expr().isIdentity() ? ctx.deRef(refValue.expr().refs().get(0)) : refValue;
        newValues.add(usedValue);
      } else return false;
    }

    zipForEach(refs(), newValues, ctx::setRef);
    return true;
  }

  protected final void stringifyAsSelectItem(Value v, StringBuilder builder, boolean compact) {
    builder.append(v.expr());
    final String str = v.toString();
    if (!str.isEmpty() && !compact) builder.append(" AS ").append(str);
  }

  protected final void stringifyRefs(StringBuilder builder, boolean compact) {
    final RefBag refs = refs();
    if (!refs.isEmpty()) {
      if (builder.charAt(builder.length() - 1) != '{') builder.append(',');
      builder.append("refs=");
      if (context() == null) builder.append(refs);
      else if (!compact) builder.append(context.deRef(refs));
      else {
        final List<Value> values = context.deRef(refs);
        final List<Value> lookup =
            predecessors.length == 1
                ? predecessors[0].values()
                : listJoin(predecessors[0].values(), predecessors[1].values());
        joining("[", "", ",", "", "]", values, builder, (v, b) -> b.append(lookup.indexOf(v)));
      }
    }
  }

  protected final void stringifyChildren(StringBuilder builder, boolean compact) {
    builder.append('(');
    joining(
        ",",
        asList(predecessors),
        builder,
        (c, b) -> {
          if (b == null) builder.append('\u25a1');
          else if (compact) c.stringifyCompact(builder);
          else c.stringify(builder);
        });
    builder.append(')');
  }

  protected abstract StringBuilder stringify0(StringBuilder builder, boolean compact);
}