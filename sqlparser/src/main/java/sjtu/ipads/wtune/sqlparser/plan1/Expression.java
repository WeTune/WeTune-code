package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.ast1.SqlContext;
import sjtu.ipads.wtune.sqlparser.ast1.SqlNode;

import java.util.List;

public interface Expression {
  /** The template of the expression. All the col-refs are replaced by placeholders. */
  SqlNode template();

  /**
   * The used col-refs in the original expression. Should only be used during value-ref solution.
   */
  List<SqlNode> colRefs();

  /** Interpolate names to placeholders. */
  SqlNode interpolate(SqlContext ctx, Values values);

  static Expression mk(SqlNode ast) {
    return new ExpressionImpl(ast);
  }
}
