package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLTableSource;
import sjtu.ipads.wtune.stmt.analyzer.NodeFinder;

import static sjtu.ipads.wtune.sqlparser.SQLTableSource.TABLE_SOURCE_KIND;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_TABLE_SOURCE;
import static sjtu.ipads.wtune.stmt.utils.StmtHelper.nodeEquals;
import static sjtu.ipads.wtune.stmt.utils.StmtHelper.nodeHash;

/** Represent a relation, e.g. either a table source or a subquery. */
public class Relation {
  private final SQLNode originalNode;
  private SQLNode generatedNode;
  private int position = -1;

  private Relation(SQLNode originalNode) {
    this.originalNode = originalNode;
  }

  public static Relation of(SQLNode node) {
    assert checkNode(node);
    return new Relation(node);
  }

  public boolean isTableSource() {
    return generatedNode != null || originalNode.type() == SQLNode.Type.TABLE_SOURCE;
  }

  public SQLNode node() {
    return generatedNode != null ? generatedNode : originalNode;
  }

  public SQLNode originalNode() {
    return originalNode;
  }

  public SQLNode generatedNode() {
    return generatedNode;
  }

  public String name() {
    if (generatedNode != null) return SQLTableSource.tableSourceName(generatedNode);
    if (isTableSource()) return SQLTableSource.tableSourceName(originalNode);
    else return "(" + originalNode.toString() + ")";
  }

  public int position() {
    return position;
  }

  /** Resolve the relation as SQLNode in given AST tree. */
  public SQLNode locateNodeIn(SQLNode root) {
    return NodeFinder.find(root, this.node());
  }

  public TableSource resolveTableSource(SQLNode root) {
    final SQLNode node = locateNodeIn(root);
    return node != null ? node.get(RESOLVED_TABLE_SOURCE) : null;
  }

  public void setGeneratedNode(SQLNode generatedNode) {
    assert generatedNode == null
        || generatedNode.type() == SQLNode.Type.INVALID
        || generatedNode.type() == SQLNode.Type.TABLE_SOURCE;
    this.generatedNode = generatedNode;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Relation relation = (Relation) o;
    return nodeEquals(originalNode, relation.originalNode);
  }

  @Override
  public int hashCode() {
    return nodeHash(originalNode);
  }

  @Override
  public String toString() {
    return originalNode.toString();
  }

  private static boolean checkNode(SQLNode node) {
    if (node == null) return false;
    if (node.type() == SQLNode.Type.QUERY) return true;
    if (node.type() == SQLNode.Type.TABLE_SOURCE) {
      final SQLTableSource.Kind kind = node.get(TABLE_SOURCE_KIND);
      return kind == SQLTableSource.Kind.SIMPLE || kind == SQLTableSource.Kind.DERIVED;
    }
    return false;
  }
}
