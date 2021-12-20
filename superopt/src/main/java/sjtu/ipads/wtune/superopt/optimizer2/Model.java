package sjtu.ipads.wtune.superopt.optimizer2;

import com.google.common.collect.Iterables;
import sjtu.ipads.wtune.common.utils.Lazy;
import sjtu.ipads.wtune.sqlparser.ast1.constants.ConstraintKind;
import sjtu.ipads.wtune.sqlparser.plan1.Expression;
import sjtu.ipads.wtune.sqlparser.plan1.PlanContext;
import sjtu.ipads.wtune.sqlparser.plan1.PlanSupport;
import sjtu.ipads.wtune.sqlparser.plan1.Value;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.superopt.constraint.Constraint;
import sjtu.ipads.wtune.superopt.constraint.Constraints;
import sjtu.ipads.wtune.superopt.fragment.Symbol;

import java.util.*;

import static sjtu.ipads.wtune.common.utils.IterableSupport.*;
import static sjtu.ipads.wtune.sqlparser.ast1.constants.ConstraintKind.*;
import static sjtu.ipads.wtune.sqlparser.plan1.PlanSupport.*;
import static sjtu.ipads.wtune.sqlparser.schema.SchemaSupport.findIC;
import static sjtu.ipads.wtune.sqlparser.schema.SchemaSupport.findRelatedIC;
import static sjtu.ipads.wtune.superopt.constraint.Constraint.Kind.Reference;
import static sjtu.ipads.wtune.superopt.fragment.Symbol.Kind.*;

class Model {
  private final Model base;
  private final PlanContext plan;
  private final Constraints constraints;
  private final Lazy<Map<Symbol, Object>> assignments;

  private Model(Model base, PlanContext plan, Constraints constraints) {
    this.base = base;
    this.plan = plan;
    this.constraints = constraints;
    this.assignments = Lazy.mk(HashMap::new);
  }

  Model(PlanContext plan, Constraints constraints) {
    this(null, plan, constraints);
  }

  Model base() {
    return base;
  }

  PlanContext plan() {
    return plan;
  }

  Constraints constraints() {
    return constraints;
  }

  Model derive() {
    return derive(plan);
  }

  Model derive(PlanContext newPlan) {
    return new Model(this, newPlan, constraints);
  }

  void reset() {
    if (assignments.isInitialized()) assignments.get().clear();
  }

  boolean isAssigned(Symbol sym) {
    return of(sym) != null;
  }

  Integer ofTable(Symbol tableSym) {
    return of(tableSym);
  }

  List<Value> ofAttrs(Symbol attrsSym) {
    return of(attrsSym);
  }

  Expression ofPred(Symbol predSym) {
    return of(predSym);
  }

  boolean assign(Symbol sym, Object assignment) {
    assert sym.kind() != TABLE || assignment instanceof Integer;
    assert sym.kind() != ATTRS || assignment instanceof List<?>;
    assert sym.kind() != PRED || assignment instanceof Expression;

    assignments.get().put(sym, assignment);

    for (Symbol eqSym : constraints.eqClassOf(sym)) {
      final Object otherAssignment = of(eqSym);
      if (otherAssignment != null && !checkCompatible(assignment, otherAssignment)) {
        return false;
      }
    }

    return true;
  }

  boolean checkConstraints() {
    return all(constraints, this::checkConstraint);
  }

  private <T> T of(Symbol sym) {
    final Object o = assignments.get().get(sym);
    if (o != null) return (T) o;
    else if (base != null) return base.of(sym);
    else return null;
  }

  private boolean checkCompatible(Object v0, Object v1) {
    if (v0.getClass() != v1.getClass()) return false;

    if (v0 instanceof Expression) return v0.toString().equals(v1.toString());

    if (v0 instanceof List) {
      final List<Value> attrs0 = (List<Value>) v0, attrs1 = (List<Value>) v1;
      if (attrs0.size() != attrs1.size()) return false;

      for (int i = 0, bound = attrs0.size(); i < bound; i++) {
        final Value attr0 = attrs0.get(i), attr1 = attrs1.get(i);
        if (!Objects.equals(attr0, attr1)
            && !Objects.equals(attr0, tryResolveRef(plan, attr1))
            && !Objects.equals(tryResolveRef(plan, attr0), attr1)) {
          return false;
        }
      }
    }

    if (v0 instanceof Integer) {
      final int id0 = ((Integer) v0), id1 = (Integer) v1;
      return PlanSupport.isEqualTree(plan, id0, plan, id1);
    }

    throw new IllegalArgumentException("unexpected assignment: " + v0);
  }

  private boolean checkConstraint(Constraint constraint) {
    switch (constraint.kind()) {
      case AttrsSub:
        return checkAttrsSub(constraint);
      case Unique:
        return checkUnique(constraint);
        //      case NotNull:
        //        return checkNotNull(constraint);
      case Reference:
        return checkReference(constraint);
      default:
        return true;
    }
  }

  private boolean checkAttrsSub(Constraint attrsSub) {
    final Symbol attrsSym = attrsSub.symbols()[0];
    final Symbol srcSym = attrsSub.symbols()[1];

    final List<Value> subAttrs = ofAttrs(attrsSym);
    if (subAttrs == null) return true;

    final List<Value> srcAttrs = ofOutAttrs(srcSym);
    if (srcAttrs == null) return true;

    return srcAttrs.containsAll(subAttrs);
  }

  private boolean checkUnique(Constraint unique) {
    final Symbol attrsSym = unique.symbols()[0];
    final Symbol srcSym = unique.symbols()[1];

    final List<Value> attrs = ofAttrs(attrsSym);
    if (attrs == null) return true; // not assigned yet, pass

    final List<Column> columns = tryResolveColumns(attrs);
    if (columns == null) return false; // some attrs has no backed column

    if (none(columns, column -> isParticipateIn(column, UNIQUE))) return false;

    final Integer input = ofTable(srcSym);
    if (input == null) return true; // not assigned yet, pass

    return PlanSupport.isUniqueCoreAt(plan, new HashSet<>(attrs), input);
  }

  private boolean checkNotNull(Constraint notNull) {
    final Symbol attrsSym = notNull.symbols()[0];
    final Symbol srcSym = notNull.symbols()[1];

    final List<Value> attrs = ofAttrs(attrsSym);
    if (attrs == null) return true; // not assigned yet, pass

    final List<Column> columns = tryResolveColumns(attrs);
    if (columns == null) return false; // some attrs has no backed column

    if (none(columns, column -> isParticipateIn(column, NOT_NULL))) return false;

    final Integer input = ofTable(srcSym);
    if (input == null) return true; // not assigned yet, pass

    return PlanSupport.isNotNullAt(plan, new HashSet<>(attrs), input);
  }

  private boolean checkReference(Constraint reference) {
    assert reference.kind() == Reference;

    final Symbol referringAttrsSym = reference.symbols()[1];
    final Symbol referredSrcSym = reference.symbols()[2];
    final Symbol referredAttrsSym = reference.symbols()[3];

    final List<Value> referringAttrs = ofAttrs(referringAttrsSym);
    if (referringAttrs == null) return true;
    final List<Column> referringColumns = tryResolveColumns(referringAttrs);
    if (referringColumns == null) return false;
    final var fks = findIC(plan.schema(), referringColumns, FOREIGN);
    if (fks.isEmpty()) return false;

    final List<Value> referredAttrs = ofAttrs(referredAttrsSym);
    if (referredAttrs == null) return true;
    final List<Column> referredCols = tryResolveColumns(referredAttrs);
    if (referredCols == null) return false;
    if (linearFind(fks, it -> it.refColumns().equals(referredCols)) == null) return false;

    final Integer surface = ofTable(referredSrcSym);
    if (surface == null) return true;

    // Check if the referred attributes are filtered, which invalidates the FK on schema.
    Value rootRef = referredAttrs.get(0);
    if (!isRootRef(plan, rootRef)) rootRef = tryResolveRef(plan, rootRef);
    assert rootRef != null;

    int path = plan.valuesReg().initiatorOf(rootRef);
    while (path != surface) {
      final int parent = plan.parentOf(path);
      assert plan.childOf(parent, 0) == path;
      if (plan.kindOf(parent).isFilter()) return false;
      path = parent;
    }

    return true;
  }

  private List<Value> ofOutAttrs(Symbol sym) {
    if (sym.kind() == TABLE) {
      final Integer nodeId = ofTable(sym);
      return nodeId == null ? null : plan.valuesReg().valuesOf(nodeId);
    } else {
      assert sym.kind() == ATTRS;
      return ofAttrs(sym);
    }
  }

  private List<Column> tryResolveColumns(List<Value> values) {
    final List<Column> columns = new ArrayList<>(values.size());
    for (Value value : values) {
      final Column column = tryResolveColumn(plan, value);
      if (column == null) return null;
      else columns.add(column);
    }
    return columns;
  }

  private boolean isParticipateIn(Column column, ConstraintKind integrityConstraint) {
    return !Iterables.isEmpty(findRelatedIC(plan.schema(), column, integrityConstraint));
  }
}
