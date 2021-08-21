package sjtu.ipads.wtune.superopt.fragment1;

import com.google.common.collect.Iterables;
import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.sqlparser.plan1.*;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.constraint.Constraints;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.function.Predicate.not;
import static sjtu.ipads.wtune.common.utils.Commons.assertFalse;
import static sjtu.ipads.wtune.common.utils.FuncUtils.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType.UNIQUE;
import static sjtu.ipads.wtune.sqlparser.schema.SchemaSupport.findIC;
import static sjtu.ipads.wtune.sqlparser.schema.SchemaSupport.findRelatedIC;

class ConstraintAwareModelImpl extends ModelImpl implements ConstraintAwareModel {
  private final PlanContext plan;
  private final Constraints constraints;

  ConstraintAwareModelImpl(PlanContext plan, Constraints constraints) {
    this(null, plan, constraints);
  }

  ConstraintAwareModelImpl(
      ConstraintAwareModelImpl base, PlanContext plan, Constraints constraints) {
    super(base);
    this.plan = plan;
    this.constraints = constraints;
  }

  @Override
  public Constraints constraints() {
    return constraints;
  }

  @Override
  public void reset() {
    assignments.clear();
  }

  @Override
  public Model derive() {
    return new ConstraintAwareModelImpl(this, plan, constraints);
  }

  @Override
  protected boolean assign0(Symbol target, Object newVal) {
    for (Symbol other : constraints.eqClassOf(target)) {
      final Object oldVal = get0(other);
      if (oldVal != null && !checkCompatible(newVal, oldVal)) return false;
      if (shouldTransitiveAssign(target, other, newVal, oldVal)) super.assign0(target, newVal);
    }
    return true;
  }

  @Override
  public boolean checkConstraint() {
    final Set<Symbol> assignedSymbols = assignments.keySet();
    for (Constraint constraint : constraints)
      if (any(asList(constraint.symbols()), assignedSymbols::contains))
        if (!checkConstraint(constraint)) {
          return false;
        }

    return true;
  }

  private boolean checkCompatible(Object newOne, Object oldOne) {
    if (oldOne == newOne) {
      return true;
    } else if (oldOne instanceof PlanNode || oldOne instanceof Expr) {
      return oldOne.toString().equals(newOne.toString());
    } else if (oldOne instanceof Pair) {
      final List<Value> oldAttrs = ((Pair<List<Value>, ?>) oldOne).getLeft();
      final List<Value> newAttrs = ((Pair<List<Value>, ?>) newOne).getLeft();
      return oldAttrs.equals(newAttrs);
    } else {
      return assertFalse();
    }
  }

  private boolean checkConstraint(Constraint constraint) {
    switch (constraint.kind()) {
      case AttrsSub:
        return checkAttrSub(constraint);
      case Unique:
        return checkUnique(constraint);
      case Reference:
        return checkReference(constraint);
      default:
        return true;
    }
  }

  private boolean checkAttrSub(Constraint attrsSub) {
    assert attrsSub.kind() == Constraint.Kind.AttrsSub;
    final List<Value> attrs = interpretInAttrs(attrsSub.symbols()[0]);
    if (attrs == null) return true;

    final Symbol src = attrsSub.symbols()[1];
    if (src.kind() == Symbol.Kind.TABLE) {
      final PlanNode inputNode = interpretTable(src);
      return inputNode == null || inputNode.values().containsAll(attrs);
    } else {
      final var pair = interpretAttrs(src);
      return pair == null || pair.getRight().containsAll(attrs);
    }
  }

  private boolean checkUnique(Constraint unique) {
    assert unique.kind() == Constraint.Kind.Unique;

    final List<Value> attrs = interpretInAttrs(unique.symbols()[1]);
    if (attrs == null) return true;

    final List<Column> columns = resolveSourceColumns(attrs);
    if (columns == null) return false;
    for (Column column : columns)
      if (Iterables.isEmpty(findRelatedIC(plan.schema(), column, UNIQUE))) return false;

    final PlanNode surface = interpretTable(unique.symbols()[0]);
    if (surface == null) return true;

    return isUniqueCoreOf(new HashSet<>(attrs), surface);
  }

  private boolean checkReference(Constraint reference) {
    assert reference.kind() == Constraint.Kind.Reference;

    final List<Value> referringAttrs = interpretInAttrs(reference.symbols()[1]);
    if (referringAttrs == null) return true;

    final List<Column> referringColumns = resolveSourceColumns(referringAttrs);
    if (referringColumns == null) return false;

    // Check integrity constraint on schema.
    final var fk = findIC(plan.schema(), referringColumns, ConstraintType.FOREIGN);
    if (fk == null) return false;

    final List<Value> referredAttrs = interpretInAttrs(reference.symbols()[3]);
    if (referredAttrs == null) return true;
    if (!fk.refColumns().equals(resolveSourceColumns(referredAttrs))) return false;

    // Check if the referred attributes are filtered, which invalidates the FK on schema.
    final PlanNode surface = interpretTable(reference.symbols()[1]);
    if (surface == null) return true;

    final PlanNode inputNode = plan.ownerOf(plan.sourceOf(referredAttrs.get(0)));
    assert inputNode.kind() == OperatorType.INPUT;

    PlanNode path = inputNode;
    while (path != surface) {
      if (path.successor().predecessors()[0] == path && path.successor().kind().isFilter())
        return false;
      path = path.successor();
    }

    return true;
  }

  private boolean shouldTransitiveAssign(Symbol from, Symbol to, Object newVal, Object oldVal) {
    assert from.kind() == to.kind();

    if (newVal == oldVal) return false;

    if (oldVal == null) return true;
    if (from.ctx() == to.ctx()) return false;
    if (from.kind() != Symbol.Kind.ATTRS) return false;

    assert oldVal instanceof Pair && newVal instanceof Pair;
    return ((Pair<?, ?>) oldVal).getRight() == null && ((Pair<?, ?>) newVal).getRight() != null;
  }

  private List<Column> resolveSourceColumns(List<Value> attrs) {
    final List<Column> columns = new ArrayList<>(attrs.size());

    for (Value attr : attrs) {
      final Value srcAttr = plan.sourceOf(attr);
      if (srcAttr.column() == null) return null; // conservative reject
      else columns.add(srcAttr.column());
    }

    return columns;
  }

  private boolean isUniqueCoreOf(Set<Value> attrs, PlanNode surface) {
    final OperatorType kind = surface.kind();

    if (kind.isJoin()) {
      final JoinNode join = (JoinNode) surface;
      final ValueBag lhsValues = join.predecessors()[0].values();
      final Set<Value> lhs = setFilter(attrs, lhsValues::contains);
      final Set<Value> rhs = setFilter(attrs, not(lhs::contains));

      if (join.isEquiJoin()) {
        final List<Value> lhsJoinAttrs = plan.deRef(join.lhsRefs());
        final List<Value> rhsJoinAttrs = plan.deRef(join.rhsRefs());
        for (int i = 0, bound = lhsJoinAttrs.size(); i < bound; i++) {
          final Value lhsJoinAttr = lhsJoinAttrs.get(i);
          final Value rhsJoinAttr = rhsJoinAttrs.get(i);
          if (lhs.contains(lhsJoinAttr)) rhs.add(rhsJoinAttr);
          if (rhs.contains(rhsJoinAttr)) lhs.add(lhsJoinAttr);
        }
      }

      return isUniqueCoreOf(rhs, surface.predecessors()[1])
          && isUniqueCoreOf(lhs, surface.predecessors()[0]);

    } else if (kind == OperatorType.PROJ) {
      final Set<Value> srcAttrs = setMap(attrs, it -> plan.deRef(it.expr().refs().get(0)));
      return isUniqueCoreOf(srcAttrs, surface);

    } else if (kind == OperatorType.SIMPLE_FILTER) {
      final SimpleFilterNode filter = (SimpleFilterNode) surface;
      if (filter.predicate().isEquiCondition()) {
        final Value fixedAttr = plan.deRef(filter.refs().get(0));
        attrs.add(fixedAttr);
      }
      return isUniqueCoreOf(attrs, surface);

    } else if (kind == OperatorType.INPUT) {
      final InputNode inputNode = (InputNode) surface;
      final Set<Column> columns = setMap(attrs, Value::column);
      assert none(columns, Objects::isNull);

      return any(inputNode.table().constraints(UNIQUE), it -> columns.containsAll(it.columns()));

    } else {
      assert surface.predecessors().length == 1;
      return isUniqueCoreOf(attrs, surface);
    }
  }
}
