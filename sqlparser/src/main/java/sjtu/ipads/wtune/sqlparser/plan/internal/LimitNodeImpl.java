package sjtu.ipads.wtune.sqlparser.plan.internal;

import java.util.Collections;
import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDefBag;
import sjtu.ipads.wtune.sqlparser.plan.LimitNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

public class LimitNodeImpl extends PlanNodeBase implements LimitNode {
  private final ASTNode limit, offset;

  private LimitNodeImpl(ASTNode limit, ASTNode offset) {
    if (limit != null) limit = limit.deepCopy();
    if (offset != null) offset = offset.deepCopy();

    this.limit = limit;
    this.offset = offset;
  }

  public static LimitNode build(ASTNode limit, ASTNode offset) {
    return new LimitNodeImpl(limit, offset);
  }

  @Override
  public ASTNode limit() {
    return limit.deepCopy();
  }

  @Override
  public ASTNode offset() {
    return offset == null ? null : offset.deepCopy();
  }

  @Override
  public AttributeDefBag definedAttributes() {
    return predecessors()[0].definedAttributes();
  }

  @Override
  public List<AttributeDef> usedAttributes() {
    return Collections.emptyList();
  }

  @Override
  public void resolveUsed() {}

  @Override
  protected PlanNode copy0() {
    return new LimitNodeImpl(limit, offset);
  }

  @Override
  public String toString() {
    return "Limit<%s %s>".formatted(limit, offset);
  }
}
