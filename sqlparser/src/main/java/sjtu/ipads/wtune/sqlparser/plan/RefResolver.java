package sjtu.ipads.wtune.sqlparser.plan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listFilter;
import static sjtu.ipads.wtune.sqlparser.plan.ExprImpl.mkColumnRef;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.*;
import static sjtu.ipads.wtune.sqlparser.plan.ValueBag.locateValueRelaxed;

class RefResolver {
  private final PlanNode plan;
  private final PlanContext ctx;
  private StackedLookup lookup;

  private RefResolver(PlanNode plan, PlanContext ctx) {
    this.plan = requireNonNull(plan);
    this.ctx = requireNonNull(ctx);
  }

  static void resolve(PlanNode plan) {
    new RefResolver(plan, plan.context()).onNode(plan);
  }

  static void resolveSubqueryExpr(PlanNode plan) {
    new RefResolver(plan, plan.context()).resolveSubqueryExpr0(plan);
  }

  private void resolveSubqueryExpr0(PlanNode node) {
    for (PlanNode predecessor : node.predecessors()) resolveSubqueryExpr0(predecessor);
    if (node.kind() == IN_SUB_FILTER) {
      final InSubFilterNode subFilter = (InSubFilterNode) node;
      if (subFilter.rhsRefs() == null) subFilter.setRhsExpr(mkQueryExpr(node.predecessors()[1]));
    } else if (node.kind() == EXISTS_FILTER) {
      final ExistsFilterNode subFilter = (ExistsFilterNode) node;
      if (subFilter.rhsRefs() == null) subFilter.setRhsExpr(mkQueryExpr(node.predecessors()[1]));
    }
  }

  private void onNode(PlanNode node) {
    final boolean needNewLookup = needNewLookup(node);
    final boolean needStackLookup = needStackLookup(node);
    final boolean needMergeLookup = needMergeLookup(needNewLookup, node);

    assert !needStackLookup || !needNewLookup;
    assert !needStackLookup || !needMergeLookup;
    assert !needMergeLookup || needNewLookup;

    final StackedLookup currentLookup = lookup;
    if (needNewLookup) lookup = new StackedLookup(null);
    if (needStackLookup) lookup = new StackedLookup(currentLookup);

    switch (node.kind()) {
      case INPUT -> onInput((InputNode) node);
      case INNER_JOIN, LEFT_JOIN -> onJoin((JoinNode) node);
      case SIMPLE_FILTER, IN_SUB_FILTER, EXISTS_FILTER -> onFilter((FilterNode) node);
      case PROJ -> onProj((ProjNode) node);
      case AGG -> onAgg((AggNode) node);
      case SORT -> onSort((SortNode) node);
      case LIMIT -> onLimit((LimitNode) node);
      case UNION -> onUnion((SetOpNode) node);
      default -> throw failed("unsupported operator " + node.kind());
    }

    if (needMergeLookup) currentLookup.addAll(lookup.values);

    lookup = currentLookup;
  }

  private void onInput(InputNode node) {
    registerValues(node, node.values());
  }

  private void onJoin(JoinNode node) {
    onNode(node.predecessors()[0]);
    onNode(node.predecessors()[1]);

    final RefBag refs = node.refs();
    registerRefs(node, refs);
    resolveRefs(refs, false, false);

    final List<Ref> lhsRefs = new ArrayList<>(refs.size() >> 1);
    final List<Ref> rhsRefs = new ArrayList<>(refs.size() >> 1);

    for (Ref ref : refs) {
      final Value value = ctx.deRef(ref);
      if (isOnLhs(node, ctx.ownerOf(value))) lhsRefs.add(ref);
      else rhsRefs.add(ref);
    }

    if (node.isEquiJoin() && lhsRefs.size() != rhsRefs.size())
      throw failed("ill-formed equi-join: " + node);

    node.setLhsRefs(RefBag.mk(lhsRefs));
    node.setRhsRefs(RefBag.mk(rhsRefs));
    if (node.isEquiJoin()) {
      final List<Ref> reorderedRefs = new ArrayList<>(lhsRefs.size() + rhsRefs.size());
      for (int i = 0, bound = lhsRefs.size(); i < bound; i++) {
        reorderedRefs.add(lhsRefs.get(i));
        reorderedRefs.add(rhsRefs.get(i));
      }
      node.condition().setRefs(reorderedRefs);
    }
  }

  private void onFilter(FilterNode node) {
    onNode(node.predecessors()[0]);
    if (node.kind().numPredecessors() >= 2) onNode(node.predecessors()[1]);

    final RefBag refs = node.refs();
    registerRefs(node, refs);
    resolveRefs(refs, false, true);

    if (node.kind() == IN_SUB_FILTER) {
      ((InSubFilterNode) node).setRhsExpr(mkQueryExpr(node.predecessors()[1]));
    } else if (node.kind() == EXISTS_FILTER) {
      ((ExistsFilterNode) node).setRhsExpr(mkQueryExpr(node.predecessors()[1]));
    }
  }

  private void onProj(ProjNode node) {
    onNode(node.predecessors()[0]);

    if (node.containsWildcard()) {
      final ValueBag values = node.values();
      final List<Value> expanded = new ArrayList<>(values.size());
      for (Value value : values) {
        if (!(value instanceof WildcardValue)) expanded.add(value);
        else
          for (Value base : lookup(value.wildcardQualification())) {
            final Expr expr = mkColumnRef(base.qualification(), base.name());
            final ExprValue newValue = new ExprValue(base.name(), expr);
            newValue.setQualification(value.qualification());

            expanded.add(newValue);
          }
      }
      node.setValues(ValueBag.mk(expanded));
    }

    final RefBag refs = node.refs();
    registerRefs(node, refs);
    resolveRefs(refs, false, true);

    lookup.setupAux();
    registerValues(node, node.values());
  }

  private void onAgg(AggNode node) {
    onNode(node.predecessors()[0]);

    registerRefs(node, node.refs());

    resolveRefs(node.aggRefs(), false, false);
    // Refs in GROUP & HAVING clause can point to the values exposed by Aggregation
    lookup.addAll(node.values());
    resolveRefs(node.groupRefs(), true, false);
    resolveRefs(node.havingRefs(), true, false);

    lookup.clear();
    registerValues(node, node.values());

    node.setRefHints(resolveRefHints(node.refs(), node.predecessors()[0].values()));
  }

  private void onSort(SortNode node) {
    onNode(node.predecessors()[0]);

    final RefBag refs = node.refs();
    registerRefs(node, refs);
    resolveRefs(refs, false, false);

    node.setRefHints(resolveRefHints(refs, node.predecessors()[0].values()));
  }

  private void onLimit(LimitNode node) {
    onNode(node.predecessors()[0]);
  }

  private void onUnion(SetOpNode node) {
    onNode(node.predecessors()[0]);
    onNode(node.predecessors()[1]);
  }

  private void setRef(Ref ref, Value value) {
    ctx.setRef(ref, value);
  }

  private void registerValues(PlanNode node, ValueBag values) {
    ctx.registerValues(node, values);
    lookup.addAll(values);
  }

  private void registerRefs(PlanNode node, RefBag refs) {
    ctx.registerRefs(node, refs);
  }

  private void resolveRefs(Iterable<Ref> refs, boolean auxFirst, boolean recursive) {
    for (Ref ref : refs) {
      final Value value = lookup(ref, auxFirst, recursive);
      setRef(ref, value);
    }
  }

  private Value lookup(Ref ref, boolean auxFirst, boolean recursive) {
    final Value value =
        lookup.lookup(ref.intrinsicQualification(), ref.intrinsicName(), auxFirst, recursive);
    if (value == null) throw failed("unknown ref " + ref);
    return value;
  }

  private List<Value> lookup(String qualification) {
    final List<Value> values = this.lookup.lookup(qualification);
    if (values == null)
      throw failed("unknown ref " + (qualification == null ? "*" : qualification + "*"));
    return values;
  }

  private boolean isOnLhs(PlanNode root, PlanNode descent) {
    assert root.kind().numPredecessors() == 2;

    final PlanNode savedDescent = descent;
    while (descent != null) {
      if (descent.successor() == root) return root.predecessors()[0] == descent;
      descent = descent.successor();
    }

    throw failed("%s not a descent of %s".formatted(savedDescent, root));
  }

  private int[] resolveRefHints(List<Ref> refs, List<Value> inValues) {
    final List<Value> usedValues = ctx.deRef(refs);
    final int[] hints = new int[usedValues.size()];
    for (int i = 0; i < usedValues.size(); i++) {
      final Value value = locateValueRelaxed(inValues, usedValues.get(i), ctx);
      hints[i] = value == null ? -1 : inValues.indexOf(value);
    }
    return hints;
  }

  private Expr mkQueryExpr(PlanNode node) {
    return AstTranslator.translate(node, true);
  }

  private RuntimeException failed(String reason) {
    throw new IllegalArgumentException(
        "failed to bind reference to value. \"" + reason + "\" plan: " + plan);
  }

  private static boolean needNewLookup(PlanNode node) {
    final PlanNode successor = node.successor();
    if (successor == null) return true;

    final OperatorType succType = successor.kind();
    final OperatorType nodeType = node.kind();

    if (succType == UNION) return true;
    if (succType.isSubquery() && successor.predecessors()[1] == node) return false; // needStack
    if (nodeType == INPUT || nodeType.isJoin()) return false;
    if (nodeType.isFilter()) return succType.isJoin();
    if (nodeType == PROJ) return succType != AGG && succType != SORT && succType != LIMIT;
    if (nodeType == AGG) return succType != SORT && succType != LIMIT;
    if (nodeType == SORT) return succType != LIMIT;
    return nodeType == LIMIT || nodeType == UNION;
  }

  private static boolean needStackLookup(PlanNode node) {
    final PlanNode successor = node.successor();
    return successor != null
        && successor.kind().isSubquery()
        && successor.predecessors()[1] == node;
  }

  private static boolean needMergeLookup(boolean needNew, PlanNode node) {
    final PlanNode successor = node.successor();
    if (successor == null) return false;
    return needNew && (successor.kind() != UNION || node == successor.predecessors()[0]);
  }

  private static class StackedLookup {
    private final StackedLookup previous;
    private List<Value> values;
    private List<Value> auxValues;

    private StackedLookup(StackedLookup previous) {
      this.previous = previous;
      this.values = new ArrayList<>();
    }

    Value lookup(String qualification, String name, boolean auxFirst, boolean recursive) {
      final Value value0 = lookup0(qualification, name, auxValues);
      final Value value1 = lookup0(qualification, name, values);

      if (value0 != null && (value1 == null || auxFirst)) return value0;
      if (value1 != null) return value1;
      if (recursive && previous != null)
        return previous.lookup(qualification, name, auxFirst, true);

      return null;
    }

    List<Value> lookup(String qualification) {
      if (qualification == null) return values;
      else return listFilter(values, it -> qualification.equals(it.qualification()));
    }

    private static Value lookup0(String qualification, String name, List<Value> values) {
      if (name == null || name.isEmpty()) throw new IllegalArgumentException();
      if (values == null) return null;

      for (Value value : values)
        if ((qualification == null || qualification.equalsIgnoreCase(value.qualification()))
            && name.equalsIgnoreCase(value.name())) return value;

      return null;
    }

    void addAll(Collection<Value> values) {
      this.values.addAll(values);
    }

    void setupAux() {
      assert auxValues == null;
      auxValues = values;
      values = new ArrayList<>();
    }

    void clear() {
      values.clear();
    }
  }
}
