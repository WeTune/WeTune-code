package sjtu.ipads.wtune.superopt.plan.internal;

import sjtu.ipads.wtune.superopt.plan.*;
import sjtu.ipads.wtune.superopt.util.Hole;
import sjtu.ipads.wtune.symsolver.core.QueryBuilder;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static sjtu.ipads.wtune.superopt.util.Stringify.stringify;

public class PlanImpl implements Plan {
  private int id;
  public PlanNode head;
  private boolean alreadySetup;

  private final Lock l;

  private PlanImpl() {
    l = new ReentrantLock();
  }

  public static Plan build() {
    return new PlanImpl();
  }

  public static Plan build(String str) {
    final String[] opStrs = str.split("[(),]+");
    final Deque<PlanNode> planNodes = new ArrayDeque<>(opStrs.length);

    for (int i = opStrs.length - 1; i >= 0; i--) {
      final String opStr = opStrs[i];
      final String[] fields = opStr.split("[<> ]+");
      final OperatorType type = OperatorType.valueOf(fields[0]);
      final PlanNode op = type.create();

      op.setPlaceholders(fields);
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
  public void lock() {
    l.lock();
  }

  @Override
  public void unlock() {
    l.unlock();
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
  public QueryBuilder semantic() {
    setup();
    return Semantic.build(this);
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
}
