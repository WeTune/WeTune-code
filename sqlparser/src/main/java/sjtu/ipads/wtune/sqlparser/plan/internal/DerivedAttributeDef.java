package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.schema.Column;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SELECT_ITEM_ALIAS;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SELECT_ITEM_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

public class DerivedAttributeDef extends AttributeDefBase {
  private final ASTNode expr;
  private final boolean isIdentity;
  private int[] references;

  public DerivedAttributeDef(
      int id, String qualification, String name, ASTNode expr, int[] references) {
    super(id, qualification, name);
    this.expr = expr;
    this.isIdentity = COLUMN_REF.isInstance(expr);
    this.references = references == null ? null : Arrays.copyOf(references, references.length);
  }

  public static AttributeDef fromExpr(int id, String qualification, String name, ASTNode expr) {
    return new DerivedAttributeDef(id, qualification, name, expr, null);
  }

  @Override
  public boolean isIdentity() {
    return isIdentity;
  }

  @Override
  public List<AttributeDef> references() {
    return Arrays.stream(references0())
        .mapToObj(this::resolveReference)
        .collect(Collectors.toList());
  }

  @Override
  public AttributeDef upstream() {
    return isIdentity() ? resolveReference(references0()[0]) : null;
  }

  @Override
  public AttributeDef source() {
    final AttributeDef upstream = upstream();
    if (upstream == null) return this;
    else return upstream.source();
  }

  @Override
  public Column referredColumn() {
    final AttributeDef src = nativeSource();
    assert src == null || src instanceof NativeAttributeDef;
    return src != null ? src.referredColumn() : null;
  }

  @Override
  public boolean referencesTo(String qualification, String alias) {
    if ((qualification == null || qualification.equals(this.qualification()))
        && alias.equals(this.name())) return true;
    final AttributeDef upstream = upstream();
    return upstream != null && upstream.referencesTo(qualification, alias);
  }

  @Override
  public boolean referencesTo(int id) {
    return this.id() == id || (isIdentity() && upstream().referencesTo(id));
  }

  @Override
  public AttributeDef copy() {
    return new DerivedAttributeDef(id(), qualification(), name(), expr, references);
  }

  @Override
  public ASTNode toSelectItem() {
    final ASTNode item = ASTNode.node(NodeType.SELECT_ITEM);
    item.set(SELECT_ITEM_EXPR, expr.deepCopy());
    item.set(SELECT_ITEM_ALIAS, name());
    return item;
  }

  private int[] references0() {
    if (references != null) return references;

    final List<ASTNode> colRefs = gatherColumnRefs(expr);
    final int[] references = new int[colRefs.size()];

    outer:
    for (int i = 0, bound = colRefs.size(); i < bound; i++) {
      final ASTNode colRef = colRefs.get(i);
      for (PlanNode predecessor : definer().predecessors()) {
        final AttributeDef resolved = predecessor.resolveAttribute(colRef);
        if (resolved != null) {
          references[i] = resolved.id();
          continue outer;
        }
      }
      references[i] = -1;
    }

    return this.references = references;
  }

  private AttributeDef resolveReference(int id) {
    for (PlanNode predecessor : definer().predecessors()) {
      final AttributeDef resolved = predecessor.resolveAttribute(id);
      if (resolved != null) return resolved;
    }
    return null;
  }
}
