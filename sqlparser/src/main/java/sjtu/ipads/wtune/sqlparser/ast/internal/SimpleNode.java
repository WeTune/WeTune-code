package sjtu.ipads.wtune.sqlparser.ast.internal;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;

import java.util.HashMap;
import java.util.Map;

public class SimpleNode extends BaseNode implements SQLNode {
  private NodeType type;
  private Map<String, Object> directAttrs;

  private SimpleNode(NodeType type) {
    this(type, null);
  }

  private SimpleNode(NodeType type, Map<String, Object> directAttrs) {
    this.type = type;
    this.directAttrs = directAttrs;
  }

  public static SQLNode build(NodeType nodeType) {
    return new SimpleNode(nodeType);
  }

  public static SQLNode build(SQLNode other) {
    return new SimpleNode(other.nodeType(), other.directAttrs());
  }

  @Override
  public Map<String, Object> directAttrs() {
    if (directAttrs == null) directAttrs = new HashMap<>();
    return directAttrs;
  }

  @Override
  public NodeType nodeType() {
    return type;
  }

  @Override
  public void setParent(SQLNode parent) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setNodeType(NodeType type) {
    this.type = type;
  }

  @Override
  public void update(SQLNode other) {
    this.type = other.nodeType();
    this.directAttrs.clear();
    this.directAttrs.putAll(other.directAttrs());
  }
}
