package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.LimitNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanAttribute;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import java.util.Collections;
import java.util.List;

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
  public List<PlanAttribute> outputAttributes() {
    return predecessors()[0].outputAttributes();
  }

  @Override
  public List<PlanAttribute> usedAttributes() {
    return Collections.emptyList();
  }

  @Override
  public void resolveUsedAttributes() {}

  @Override
  protected PlanNode copy0() {
    return new LimitNodeImpl(limit, offset);
  }
}
