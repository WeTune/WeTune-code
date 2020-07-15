package sjtu.ipads.wtune.stmt.scriptgen;

import sjtu.ipads.wtune.stmt.schema.Constraint;

import static sjtu.ipads.wtune.common.utils.Commons.surround;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class ConstraintGen implements ScriptNode {
  private final Constraint constraint;

  public ConstraintGen(Constraint constraint) {
    this.constraint = constraint;
  }

  @Override
  public void output(Output out) {
    out.print("{ ")
        .printf("type = '%s', ", constraint.type().name().toLowerCase())
        .print("columns = { ")
        .prints(", ", listMap(it -> surround(it.columnName(), '\''), constraint.columns()))
        .print(" } ")
        .print(" }");
  }
}
