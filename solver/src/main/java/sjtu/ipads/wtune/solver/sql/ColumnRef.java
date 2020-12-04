package sjtu.ipads.wtune.solver.sql;

import sjtu.ipads.wtune.solver.node.AlgNode;
import sjtu.ipads.wtune.solver.node.TableNode;
import sjtu.ipads.wtune.solver.schema.Column;
import sjtu.ipads.wtune.solver.schema.DataType;
import sjtu.ipads.wtune.solver.sql.expr.Expr;
import sjtu.ipads.wtune.solver.sql.impl.ExprColumnRefImpl;
import sjtu.ipads.wtune.solver.sql.impl.NativeColumnRefImpl;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public interface ColumnRef {
  String alias();

  DataType dataType();

  boolean notNull();

  AlgNode owner();

  ColumnRef setOwner(AlgNode owner);

  ColumnRef setAlias(String alias);

  ColumnRef copy();

  static ColumnRef ofExpr(Expr expr, String alias, DataType dataType, boolean notNull) {
    return ExprColumnRefImpl.create(expr, alias, dataType, notNull);
  }

  static ColumnRef from(TableNode source, Column nativeCol) {
    return NativeColumnRefImpl.create(source, nativeCol);
  }

  static List<ColumnRef> from(TableNode source) {
    return listMap(it -> from(source, it), source.table().columns());
  }

}
