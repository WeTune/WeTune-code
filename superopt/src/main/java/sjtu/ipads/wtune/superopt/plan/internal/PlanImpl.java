package sjtu.ipads.wtune.superopt.plan.internal;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.plan.*;
import sjtu.ipads.wtune.superopt.util.Hole;

import java.util.ArrayDeque;
import java.util.Deque;

import static sjtu.ipads.wtune.superopt.util.Stringify.stringify;

public class PlanImpl implements Plan {
  private int id;
  public PlanNode head;
  private boolean alreadySetup;

  private final Placeholders placeholders;

  private PlanImpl() {
    placeholders = new PlaceholdersImpl();
  }

  public static Plan build() {
    return new PlanImpl();
  }

  public static Plan build(String str) {
    final String[] opStrs = str.split("[(),]+");
    final Deque<PlanNode> planNodes = new ArrayDeque<>(opStrs.length);

    for (int i = opStrs.length - 1; i >= 0; i--) {
      final String opStr = opStrs[i];
      final OperatorType type = OperatorType.valueOf(opStr.split("[<> ]+", 2)[0]);
      final PlanNode op = Operators.create(type);

      for (int j = 0; j < type.numPredecessors(); j++) op.setPredecessor(j, planNodes.pop());

      planNodes.push(op);
    }

    return Plan.wrap(planNodes.pop()).setup();
  }

  @Override
  public void setId(int id) {
    this.id = id;
  }

  @Override
  public int id() {
    return id;
  }

  @Override
  public PlanNode head() {
    return head;
  }

  @Override
  public Plan setup() {
    if (alreadySetup) return this;
    alreadySetup = true;

    for (Hole<PlanNode> hole : holes()) hole.fill(Input.create());

    acceptVisitor(PlanVisitor.traverse(it -> it.setPlan(this)));

    return this;
  }

  @Override
  public void setHead(PlanNode head) {
    this.head = head;
  }

  @Override
  public String toString() {
    return stringify(this);
  }

  @Override
  public Semantic semantic() {
    setup();
    return Semantic.build(this);
  }

  @Override
  public Placeholders placeholders() {
    return placeholders;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Plan)) return false;
    final Plan plan = (Plan) o;
    if ((this.head == null) != (plan.head() == null)) return false;
    if (this.head == null) return true;
    return this.head.structuralEquals(plan.head());
  }

  @Override
  public int hashCode() {
    return head == null ? 0 : head.structuralHash();
  }

  private static class Counter implements PlanVisitor {
    private int count;

    @Override
    public void leave(PlanNode op) {
      ++count;
    }
  }
}
