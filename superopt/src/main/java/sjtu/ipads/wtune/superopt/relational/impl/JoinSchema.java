package sjtu.ipads.wtune.superopt.relational.impl;

import sjtu.ipads.wtune.superopt.interpret.Interpretation;
import sjtu.ipads.wtune.superopt.operators.Join;
import sjtu.ipads.wtune.superopt.relational.SymbolicColumns;

public class JoinSchema extends BaseRelationSchema<Join> {
  protected JoinSchema(Join join) {
    super(join);
  }

  public static JoinSchema create(Join join) {
    return new JoinSchema(join);
  }

  @Override
  public boolean isStable() {
    return operator.prev()[0].outSchema().isStable() && operator.prev()[1].outSchema().isStable();
  }

  private SymbolicColumns columnsCache;

  @Override
  public SymbolicColumns columns(Interpretation interpretation) {
    if (columnsCache != null) return columnsCache;

    final SymbolicColumns left = operator.prev()[0].outSchema().columns(interpretation);
    final SymbolicColumns right = operator.prev()[1].outSchema().columns(interpretation);
    if (left == null || right == null) return null;

    final SymbolicColumns columns = left.concat(right);
    if (isStable()) columnsCache = columns; // cache only if stable

    return columns;
  }
}
