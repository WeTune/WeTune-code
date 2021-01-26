package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.TableSourceAttr;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;

import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.DERIVED_SOURCE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.SIMPLE_SOURCE;

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
    if (isTableSource()) return TableSourceAttr.tableSourceName(originalNode);
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
      return SIMPLE_SOURCE.isInstance(node) || DERIVED_SOURCE.isInstance(node);
    return false;
  }
}
