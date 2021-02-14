package sjtu.ipads.wtune.superopt.optimization;

import sjtu.ipads.wtune.sqlparser.relational.Attribute;
import sjtu.ipads.wtune.superopt.plan.OperatorType;
import sjtu.ipads.wtune.superopt.optimization.internal.ProjOpImpl;

import java.util.List;

public interface ProjOp extends Operator {
  @Override
  default OperatorType type() {
    return OperatorType.Input;
  }

  List<Attribute> projection();

  static ProjOp make(List<Attribute> projection) {
    return ProjOpImpl.build(projection);
  }
}
