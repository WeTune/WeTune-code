package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.LimitNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanAttribute;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class LimitNodeImpl extends PlanNodeBase implements LimitNode {
  private final ASTNode limit, offset;

  private LimitNodeImpl(ASTNode limit, ASTNode offset) {
    this.limit = limit;
    this.offset = offset;
  }

  public static LimitNode build(ASTNode limit, ASTNode offset) {
    return new LimitNodeImpl(limit, offset);
  }

  @Override
  public ASTNode limit() {
    return limit;
  }

  @Override
  public ASTNode offset() {
    return offset;
  }

  @Override
  public List<PlanAttribute> definedAttributes() {
    return predecessors()[0].definedAttributes();
  }

  @Override
  public List<PlanAttribute> usedAttributes() {
    return Collections.emptyList();
  }

  @Override
  public void resolveUsedTree() {}

  @Override
  protected PlanNode copy0() {
    return new LimitNodeImpl(limit, offset);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LimitNodeImpl limitNode = (LimitNodeImpl) o;
    return Objects.equals(limit, limitNode.limit) && Objects.equals(offset, limitNode.offset);
  }

  @Override
  public int hashCode() {
    return Objects.hash(limit, offset);
  }
}
