package sjtu.ipads.wtune.sql.support.resolution;

import sjtu.ipads.wtune.common.field.FieldKey;
import sjtu.ipads.wtune.sql.ast.SqlContext;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.sql.schema.Schema;
import sjtu.ipads.wtune.sql.schema.Table;

import static sjtu.ipads.wtune.common.tree.TreeSupport.nodeEquals;
import static sjtu.ipads.wtune.common.utils.Commons.tail;
import static sjtu.ipads.wtune.common.utils.IterableSupport.any;
import static sjtu.ipads.wtune.common.utils.IterableSupport.linearFind;
import static sjtu.ipads.wtune.sql.ast.ExprFields.ColRef_ColName;
import static sjtu.ipads.wtune.sql.ast.ExprKind.ColRef;
import static sjtu.ipads.wtune.sql.ast.SqlKind.*;
import static sjtu.ipads.wtune.sql.ast.SqlNodeFields.*;
import static sjtu.ipads.wtune.sql.ast.TableSourceFields.Joined_On;
import static sjtu.ipads.wtune.sql.ast.TableSourceFields.Simple_Table;
import static sjtu.ipads.wtune.sql.ast.TableSourceKind.JoinedSource;
import static sjtu.ipads.wtune.sql.ast.TableSourceKind.SimpleSource;
import static sjtu.ipads.wtune.sql.support.resolution.ParamModifier.Type.*;
import static sjtu.ipads.wtune.sql.support.resolution.Params.PARAMS;
import static sjtu.ipads.wtune.sql.support.resolution.Relations.RELATION;

public abstract class ResolutionSupport {
  private ResolutionSupport() {}

  static boolean limitClauseAsParam = false;

  static int scopeRootOf(SqlNode node) {
    if (Relation.isRelationRoot(node)) return node.nodeId();

    final SqlContext ctx = node.context();
    int cursor = node.nodeId();
    while (ctx.kindOf(cursor) != Query) cursor = ctx.parentOf(cursor);
    return cursor;
  }

  public static void setLimitClauseAsParam(boolean limitClauseAsParam) {
    ResolutionSupport.limitClauseAsParam = limitClauseAsParam;
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

  public static Table tableOf(Relation relation) {
    if (!isDirectTable(relation)) return null;
    final Schema schema = relation.rootNode().context().schema();
    final String tableName = relation.rootNode().$(Simple_Table).$(TableName_Table);
    return schema.table(tableName);
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

  public static ParamDesc paramOf(SqlNode node) {
    return node.context().getAdditionalInfo(PARAMS).paramOf(node);
  }

  public static Relation getOuterRelation(Relation relation) {
    final SqlNode parent = relation.rootNode().parent();
    return parent == null ? null : getEnclosingRelation(parent);
  }

  public static Attribute resolveAttribute(Relation relation, String qualification, String name) {
    for (Relation input : relation.inputs()) {
      final Attribute attr = input.resolveAttribute(qualification, name);
      if (attr != null) return attr;
    }
    return null;
  }

  public static Attribute resolveAttribute(SqlNode colRef) {
    if (!ColRef.isInstance(colRef)) return null;

    final Relation relation = getEnclosingRelation(colRef);
    final String qualification = colRef.$(ColRef_ColName).$(ColName_Table);
    final String name = colRef.$(ColRef_ColName).$(ColName_Col);

    Attribute attr = resolveAttribute(relation, qualification, name);
    if (attr != null) return attr;

    if (qualification == null) {
      final FieldKey<?> clause = getClauseOfExpr(colRef);
      if (clause == Query_OrderBy || clause == QuerySpec_GroupBy || clause == QuerySpec_Having) {
        attr = linearFind(relation.attributes(), it -> name.equals(it.name()));
        if (attr != null) return attr;
      }
    }

    // dependent col-ref
    Relation outerRelation = relation;
    while (tableSourceOf(outerRelation) == null
        && (outerRelation = getOuterRelation(outerRelation)) != null) {
      attr = resolveAttribute(outerRelation, qualification, name);
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

  public static boolean isElementParam(ParamDesc param) {
    return any(param.modifiers(), it -> it.type() == ARRAY_ELEMENT || it.type() == TUPLE_ELEMENT);
  }

  public static boolean isCheckNull(ParamDesc param) {
    final ParamModifier.Type lastModifierType = tail(param.modifiers()).type();
    return lastModifierType == CHECK_NULL || lastModifierType == CHECK_NULL_NOT;
  }

  private static FieldKey<?> getClauseOfExpr(SqlNode expr) {
    assert Expr.isInstance(expr);

    SqlNode parent = expr.parent(), child = expr;
    while (Expr.isInstance(parent)) {
      child = parent;
      parent = parent.parent();
    }

    if (OrderItem.isInstance(parent)) return Query_OrderBy;
    if (GroupItem.isInstance(parent)) return QuerySpec_GroupBy;
    if (SelectItem.isInstance(parent)) return QuerySpec_SelectItems;
    if (JoinedSource.isInstance(parent)) return Joined_On;
    if (nodeEquals(parent.$(Query_Offset), child)) return Query_Offset;
    if (nodeEquals(parent.$(Query_Limit), child)) return Query_Offset;
    if (nodeEquals(parent.$(QuerySpec_Where), child)) return QuerySpec_Where;
    if (nodeEquals(parent.$(QuerySpec_Having), child)) return QuerySpec_Having;

    return null;
  }
}
