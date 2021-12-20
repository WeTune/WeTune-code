package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.Commons.joining;
import static sjtu.ipads.wtune.common.utils.ListSupport.flatMap;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.WILDCARD_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SELECT_ITEM_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.TABLE_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.WILDCARD;

class ProjNodeImpl extends PlanNodeBase implements ProjNode {
  private boolean explicitDistinct;
  private ValueBag values;
  private RefBag refs;

  private boolean containsWildcard;

  ProjNodeImpl(boolean explicitDistinct, ValueBag values, RefBag refs) {
    this.explicitDistinct = explicitDistinct;
    this.values = requireNonNull(values);
    this.refs = requireNonNull(refs);
    this.containsWildcard = containsWildcard(values);
  }

  static ProjNode mk(boolean explicitDistinct, List<ASTNode> selectItems) {
    final List<Value> values = new ArrayList<>(selectItems.size());
    final List<Ref> refs = new ArrayList<>(selectItems.size());

    for (ASTNode selectItem : selectItems) {
      final ASTNode exprNode = selectItem.get(SELECT_ITEM_EXPR);
      if (WILDCARD.isInstance(exprNode)) {
        // Wildcard value will be expanded during ref-binding.
        final ASTNode tableName = exprNode.get(WILDCARD_TABLE);
        final String qualification = tableName == null ? null : tableName.get(TABLE_NAME_TABLE);
        values.add(new WildcardValue(qualification));

      } else {
        // "" indicates an anonymous value
        // such value cannot be referenced *by name* elsewhere in the query.
        // e.g., select sub.* from (select T.x + 1 from T) sub
        // Although "T.x + 1" are included in the selection because of wildcard,
        // it can never be used in the WHERE
        final ExprValue value = ExprValue.fromSelectItem(selectItem);
        values.add(value);
        refs.addAll(value.expr().refs());
      }
    }

    return new ProjNodeImpl(explicitDistinct, ValueBag.mk(values), RefBag.mk(refs));
  }

  // Make a Proj node that output `outValues`.
  static ProjNode mk(ValueBag outValues) {
    return new ProjNodeImpl(
        false, outValues, RefBag.mk(flatMap(outValues, it -> it.expr().refs())));
  }

  // Make a Proj node that select all of `inValues`.
  // Each of the Proj's output is an identity ExprValue,
  static ProjNode mkWildcard(ValueBag inValues) {
    final List<Ref> refs = new ArrayList<>(inValues.size());
    final List<Value> outValues = new ArrayList<>(inValues.size());
    for (Value inValue : inValues) {
      final Ref ref = inValue.selfish();
      final Expr expr = Expr.mk(RefBag.mk(singletonList(ref)));
      final Value outValue = Value.mk(null, inValue.name(), expr);
      refs.add(ref);
      outValues.add(outValue);
    }
    return new ProjNodeImpl(false, ValueBag.mk(outValues), RefBag.mk(refs));
  }

  @Override
  public ValueBag values() {
    return values;
  }

  @Override
  public RefBag refs() {
    return refs;
  }

  @Override
  public boolean containsWildcard() {
    return containsWildcard;
  }

  @Override
  public boolean isDeduplicated() {
    return explicitDistinct;
  }

  @Override
  public PlanNode copy(PlanContext ctx) {
    checkContextSet();

    final ProjNode copy = new ProjNodeImpl(explicitDistinct, values, this.refs);
    copy.setContext(ctx);

    ctx.registerRefs(copy, refs);
    ctx.registerValues(copy, values);
    for (Ref ref : refs) ctx.setRef(ref, this.context.deRef(ref));

    return copy;
  }

  @Override
  public void setValues(ValueBag values) {
    if (!containsWildcard)
      throw new IllegalStateException("the values are immutable if there are no wildcard");
    this.values = requireNonNull(values);
    // Values are expected containing only ExprValue. We do this check optimistically.
    this.refs = RefBag.mk(flatMap(values, it -> it.expr().refs()));
    this.containsWildcard = containsWildcard(values);
  }

  @Override
  public void setDeduplicated(boolean explicitDistinct) {
    this.explicitDistinct = explicitDistinct;
  }

  private static boolean containsWildcard(ValueBag values) {
    return values.stream().anyMatch(it -> it instanceof WildcardValue);
  }

  @Override
  public StringBuilder stringify0(StringBuilder builder, boolean compact) {
    builder.append("Proj{");

    builder.append('[');
    joining(",", values, builder, (v, b) -> stringifyAsSelectItem(v, b, compact));
    builder.append(']');

    if (explicitDistinct) builder.append(",distinct");

    stringifyRefs(builder, compact);

    builder.append('}');

    stringifyChildren(builder, compact);

    return builder;
  }
}
