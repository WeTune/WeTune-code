package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.TableSourceAttrs;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;

import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.DERIVED;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.SIMPLE;

/** Represent a relation, e.g. either a table source or a subquery. */
public class Relation {
  private final SQLNode originalNode;
  private int position = Integer.MIN_VALUE;

  private Relation(SQLNode originalNode) {
    this.originalNode = originalNode;
  }

  public static Relation of(SQLNode node) {
    assert checkNode(node);
    return new Relation(node);
  }

  public boolean isTableSource() {
    return originalNode.nodeType() == NodeType.TABLE_SOURCE;
  }

  public SQLNode node() {
    return originalNode;
  }

  public String name() {
    if (isTableSource()) return TableSourceAttrs.tableSourceName(originalNode);
    else return "(" + originalNode.toString() + ")";
  }

  public int position() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Relation relation = (Relation) o;
    return originalNode == relation.originalNode;
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(originalNode);
  }

  @Override
  public String toString() {
    return originalNode.toString();
  }

  private static boolean checkNode(SQLNode node) {
    if (node == null) return false;
    if (node.nodeType() == NodeType.QUERY) return true;
    if (node.nodeType() == NodeType.TABLE_SOURCE)
      return SIMPLE.isInstance(node) || DERIVED.isInstance(node);
    return false;
  }
}
