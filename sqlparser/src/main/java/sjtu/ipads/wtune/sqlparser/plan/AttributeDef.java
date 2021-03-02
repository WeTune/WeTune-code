package sjtu.ipads.wtune.sqlparser.plan;

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
  PlanNode definer();

  String qualification();

  String name();

  int id();

  int[] references();

  // invariant: isIdentity <=> upstream != null
  boolean isIdentity();

  AttributeDef upstream();

  // invariant: nativeUpstream != null <=> referredColumn != null
  AttributeDef nativeUpstream();

  AttributeDef copy();

  Column referredColumn();

  ASTNode toColumnRef();

  ASTNode toSelectItem();

  void setQualification(String qualification);

  void setDefiner(PlanNode definer);

  boolean isReferencedBy(String qualification, String alias);

  boolean isReferencedBy(int id);

  static AttributeDef fromColumn(int id, String tableAlias, Column c) {
    return NativeAttributeDef.fromColumn(id, tableAlias, c);
  }

  static AttributeDef fromExpr(int id, String qualification, String name, ASTNode expr) {
    return DerivedAttributeDef.fromExpr(id, qualification, name, expr);
  }
}
