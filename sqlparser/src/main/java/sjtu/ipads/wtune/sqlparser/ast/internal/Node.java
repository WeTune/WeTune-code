package sjtu.ipads.wtune.sqlparser.ast.internal;

import sjtu.ipads.wtune.sqlparser.SQLContext;
import sjtu.ipads.wtune.sqlparser.ast.Formatter;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.SQLVisitor;
import sjtu.ipads.wtune.sqlparser.rel.Relation;

public abstract class Node implements SQLNode {
  protected Relation relation;

  @Override
  public SQLContext context() {
    return null;
  }

  @Override
  public Relation relation() {
    return relation != null ? relation : parent() != null ? (relation = parent().relation()) : null;
  }

  @Override
  public SQLNode parent() {
    return null;
  }

  @Override
  public void setRelation(Relation relation) {
    this.relation = relation;
  }

  @Override
  public void accept(SQLVisitor visitor) {
    if (VisitorController.enter(this, visitor)) VisitorController.visitChildren(this, visitor);
    VisitorController.leave(this, visitor);
  }

  @Override
  public String toString() {
    return toString(true);
  }

  @Override
  public String toString(boolean oneline) {
    final Formatter formatter = new Formatter(oneline);
    accept(formatter);
    return formatter.toString();
  }
}
