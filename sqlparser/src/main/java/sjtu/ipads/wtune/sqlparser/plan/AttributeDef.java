package sjtu.ipads.wtune.sqlparser.plan;

import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.DerivedAttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.internal.NativeAttributeDef;
import sjtu.ipads.wtune.sqlparser.schema.Column;

/**
 * This class describes a reference to an attribute.
 *
 * <p>The semantic of an attribute-def is two-fold: First, it represents an named attribute ({@link
 * #qualification()} & {@link #name()}). Second, it can be viewed as a function which takes an tuple
 * as input, and output a new tuple that contains a single attribute.
 *
 * <p>The schema of the output relation of each plan node is just a list of attribute-defs ({@link
 * PlanNode#definedAttributes()}. Each node may use some attributes to fulfill its semantic ({@link
 * PlanNode#usedAttributes()}.
 *
 * <p>Each attribute-def appears in a query plan is uniquely identified ({@link #id()}.
 */
public interface AttributeDef {
  int id();

  String qualification();

  String name();

  boolean isNative();

  /**
   * Whether this Def is an identity function, i.e. a plain ColumnRef.
   *
   * <p>invariant: isIdentity <=> upstream != null
   */
  boolean isIdentity();

  /**
   * All Defs referenced by this Def.
   *
   * <p>e.g. a.x + b.y AS `sum`, `sum`.references() => [a.x,b.y]
   */
  List<AttributeDef> references();

  /**
   * The single Def referenced by this Def.
   *
   * <p>This is a convenient and efficient version of {@link #references()} when this.{@link
   * #isIdentity()} is true. If !this.{@link #isIdentity()}, null is returned.
   */
  AttributeDef upstream();

  /** The recursive upstream of this Def. */
  AttributeDef source();

  AttributeDef copy();

  Column referredColumn();

  ASTNode makeColumnRef();

  ASTNode makeSelectItem();

  AttributeDef copyWithQualification(String qualification);

  void setReferences(List<AttributeDef> references);

  boolean referencesTo(String qualification, String alias);

  boolean referencesTo(int id);

  // invariant: nativeUpstream != null <=> referredColumn != null
  default AttributeDef nativeSource() {
    final AttributeDef source = source();
    if (source instanceof NativeAttributeDef) return source;
    else return null;
  }

  static PlanNode locateDefiner(AttributeDef attr, PlanNode root) {
    final PlanNode definer = locateDefiner0(attr, root);
    if (definer == null) throw new PlanException("cannot find definer");
    return definer;
  }

  static PlanNode localeInput(AttributeDef attr, PlanNode root) {
    if (attr.nativeSource() == null) return null;

    final PlanNode definer = locateDefiner(attr, root);
    if (definer == null) return null;
    if (definer.kind() == OperatorType.INPUT) return definer;

    assert definer.kind() == OperatorType.AGG || definer.kind() == OperatorType.PROJ;
    return localeInput(attr.upstream(), definer.predecessors()[0]);
  }

  private static PlanNode locateDefiner0(AttributeDef attr, PlanNode root) {
    switch (root.kind()) {
      case SIMPLE_FILTER:
      case IN_SUB_FILTER:
      case SORT:
      case LIMIT:
        return locateDefiner0(attr, root.predecessors()[0]);

      case INPUT:
      case AGG:
      case PROJ:
        if (isDefinedBy(attr, root)) return root;
        else return null;

      case INNER_JOIN:
      case LEFT_JOIN:
        final PlanNode leftDefiner = locateDefiner0(attr, root.predecessors()[0]);
        return leftDefiner != null ? leftDefiner : locateDefiner0(attr, root.predecessors()[1]);

      default:
        throw new IllegalArgumentException();
    }
  }

  private static boolean isDefinedBy(AttributeDef attr, PlanNode node) {
    for (AttributeDef defined : node.definedAttributes())
      if (defined.id() == attr.id()) return true;
    return false;
  }

  static AttributeDef fromColumn(int id, String tableAlias, Column c) {
    return NativeAttributeDef.build(id, tableAlias, c);
  }

  static AttributeDef fromExpr(int id, String qualification, String name, ASTNode expr) {
    return DerivedAttributeDef.build(id, qualification, name, expr);
  }
}
