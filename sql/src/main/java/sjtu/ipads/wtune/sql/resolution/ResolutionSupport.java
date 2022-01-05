package sjtu.ipads.wtune.sql.resolution;

import sjtu.ipads.wtune.sql.ast1.SqlContext;
import sjtu.ipads.wtune.sql.ast1.SqlNode;

import static sjtu.ipads.wtune.common.tree.TreeSupport.nodeEquals;
import static sjtu.ipads.wtune.sql.ast1.ExprFields.ColRef_ColName;
import static sjtu.ipads.wtune.sql.ast1.ExprKind.ColRef;
import static sjtu.ipads.wtune.sql.ast1.SqlKind.*;
import static sjtu.ipads.wtune.sql.ast1.SqlNodeFields.*;
import static sjtu.ipads.wtune.sql.ast1.TableSourceKind.SimpleSource;
import static sjtu.ipads.wtune.sql.resolution.Relations.RELATION;

public abstract class ResolutionSupport {
  private ResolutionSupport() {}

  static int scopeRootOf(SqlNode node) {
    if (Relation.isRelationRoot(node)) return node.nodeId();

    final SqlContext ctx = node.context();
    int cursor = node.nodeId();
    while (ctx.kindOf(cursor) != Query) cursor = ctx.parentOf(cursor);
    return cursor;
  }

  public static Relation getEnclosingRelation(SqlNode node) {
    return node.context().getAdditionalInfo(RELATION).enclosingRelationOf(node);
  }

  public static boolean isDirectTable(Relation relation) {
    return SimpleSource.isInstance(relation.rootNode());
  }

  public static boolean isServedAsInput(Relation relation) {
    final SqlNode node = relation.rootNode();
    final SqlNode parent = node.parent();
    return TableSource.isInstance(node)
        || TableSource.isInstance(parent)
        || SetOp.isInstance(parent) && nodeEquals(parent.$(SetOp_Left), node);
  }

  public static SqlNode tableSourceOf(Relation relation) {
    final SqlNode node = relation.rootNode();
    if (TableSource.isInstance(node)) return node;
    final SqlNode parent = node.parent();
    if (TableSource.isInstance(parent)) return parent;
    return null;
  }

  public static SqlNode queryOf(Relation relation) {
    final SqlNode node = relation.rootNode();
    if (Query.isInstance(node)) return node;
    else return null;
  }

  public static Relation getOuterRelation(Relation relation) {
    final SqlNode parent = relation.rootNode().parent();
    return parent == null ? null : getEnclosingRelation(parent);
  }

  public static Attribute resolveAttribute(SqlNode colRef) {
    if (!ColRef.isInstance(colRef)) return null;

    final String qualification = colRef.$(ColRef_ColName).$(ColName_Table);
    final String name = colRef.$(ColRef_ColName).$(ColName_Col);
    final Relation relation = getEnclosingRelation(colRef);
    for (Relation input : relation.inputs()) {
      final Attribute attr = input.resolveAttribute(qualification, name);
      if (attr != null) return attr;
    }

    return null;
  }

  public static Attribute deRef(Attribute attribute) {
    final SqlNode expr = attribute.expr();
    return ColRef.isInstance(expr) ? resolveAttribute(expr) : null;
  }

  public static Attribute traceRef(Attribute attribute) {
    final Attribute ref = deRef(attribute);
    return ref == null ? attribute : traceRef(ref);
  }
}
