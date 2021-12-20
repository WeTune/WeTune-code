package sjtu.ipads.wtune.superopt.optimizer2;

import sjtu.ipads.wtune.sqlparser.plan.Value;

import java.util.List;

class AttrsAssignment {
  final List<Value> values;

  AttrsAssignment(List<Value> values) {
    this.values = values;
  }
}
