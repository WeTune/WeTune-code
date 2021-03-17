package sjtu.ipads.wtune.sqlparser.plan.internal;

import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SELECT_ITEM_ALIAS;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SELECT_ITEM_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.schema.Column;

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
    return this.id() == id
        || (isIdentity() && (references0()[0] == id || upstream().referencesTo(id)));
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AttributeDef)) return false;
    if (this.id() == ((AttributeDef) o).id()) return true;

    if (o instanceof NativeAttributeDef) {
      if (!isIdentity()) return false;
      final int[] refs = references0();
      if (refs[0] == ((NativeAttributeDef) o).id()) return true; // shortcut
      return o.equals(nativeSource());
    }

    if (o instanceof DerivedAttributeDef) {
      final DerivedAttributeDef that = (DerivedAttributeDef) o;
      if (this.isIdentity()) {
        final int[] refs = references0();
        if (refs[0] == that.id()) return true; // shortcut
      }
      if (that.isIdentity()) {
        final int[] refs = that.references0();
        if (refs[0] == this.id()) return true; // shortcut
      }
      return this.source().id() == that.source().id();
    }

    return false;
  }

  // fast but not precisely check
  // if this method returns true, then must have x.equals(y) == true
  // if x.equals(y) == false, then this method must return false
  public static boolean fastEquals(AttributeDef x, AttributeDef y) {
    if (x.id() == y.id()) return true;
    final boolean xIsNative = x instanceof NativeAttributeDef;
    final boolean yIsNative = y instanceof NativeAttributeDef;
    if (xIsNative && yIsNative) return x.equals(y);
    final int xRef =
        (!xIsNative && x.isIdentity()) ? ((DerivedAttributeDef) x).references0()[0] : -1;
    final int yRef =
        (!yIsNative && y.isIdentity()) ? ((DerivedAttributeDef) y).references0()[0] : -1;
    return xRef == y.id() || yRef == x.id() || (xRef != -1 && xRef == yRef);
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
    // fast path
    for (PlanNode predecessor : definer().predecessors())
      for (AttributeDef outAttr : predecessor.definedAttributes())
        if (outAttr.id() == id) return outAttr;

    // slow path, should be rare case
    for (PlanNode predecessor : definer().predecessors()) {
      final AttributeDef resolved = predecessor.resolveAttribute(id);
      if (resolved != null) return resolved;
    }
    return null;
  }
}
