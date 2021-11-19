package sjtu.ipads.wtune.sqlparser.plan1;

public interface InSubNode extends PlanNode {
  Expression expr();

  @Override
  default PlanKind kind() {
    return PlanKind.InSub;
  }

  static InSubNode mk(Expression expr) {
    return new InSubNodeImpl(expr);
  }
}
