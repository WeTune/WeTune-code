package sjtu.ipads.wtune.sqlparser.plan1;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.Commons.head;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listFlatMap;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.WILDCARD_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SELECT_ITEM_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.TABLE_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.WILDCARD;

import java.util.ArrayList;
import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

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

  static ProjNode build(boolean explicitDistinct, List<ASTNode> selectItems) {
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

    return new ProjNodeImpl(explicitDistinct, new ValueBagImpl(values), new RefBagImpl(refs));
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
  public boolean isExplicitDistinct() {
    return explicitDistinct;
  }

  @Override
  protected PlanNode copy0(PlanContext ctx) {
    checkContextSet();

    final ProjNode copy = new ProjNodeImpl(explicitDistinct, values, this.refs);
    copy.setContext(ctx);

    ctx.registerRefs(this, refs);
    ctx.registerValues(this, values);
    for (Ref ref : refs) ctx.setRef(ref, this.context.deRef(ref));

    return copy;
  }

  @Override
  public void setValues(ValueBag values) {
    if (!containsWildcard)
      throw new IllegalStateException("the values are immutable if there are no wildcard");
    this.values = requireNonNull(values);
    // Values are expected containing only ExprValue. We do this check optimistically.
    this.refs = new RefBagImpl(listFlatMap(it -> it.expr().refs(), values));
    this.containsWildcard = containsWildcard(values);
  }

  @Override
  public void setExplicitDistinct(boolean explicitDistinct) {
    this.explicitDistinct = explicitDistinct;
  }

  private static boolean containsWildcard(ValueBag values) {
    return values.stream().anyMatch(it -> it instanceof WildcardValue);
  }

  private static final int LIMIT = 1;

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder("Proj{").append('[');
    int count = 0;
    for (Value value : values) {
      if (count++ >= LIMIT) break;
      if (value.expr() != null) builder.append(value.expr());
      final String str = value.toString();
      if (!str.isEmpty()) {
        if (value.expr() != null) builder.append(" AS ");
        builder.append(str);
      }
    }
    builder.append(']');
    if (values.size() > LIMIT) builder.append("...");
    if (explicitDistinct) builder.append(",distinct");
    if (!refs.isEmpty()) {
      builder.append(",refs=");
      if (context == null) builder.append(head(refs));
      else builder.append(context.deRef(refs));
      if (refs.size() > LIMIT) builder.append("...");
    }
    builder.append('}');

    if (predecessors[0] != null) builder.append('(').append(predecessors[0]).append(')');

    return builder.toString();
  }
}
