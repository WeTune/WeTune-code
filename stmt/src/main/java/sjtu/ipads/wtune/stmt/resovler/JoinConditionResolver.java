package sjtu.ipads.wtune.stmt.resovler;

import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.attrs.ColumnRef;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.Set;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.BOOL_EXPR;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_COLUMN_REF;

/**
 * Marking all join conditions. Essentially, a join condition is used to eq-join two tables.
 *
 * <p><b>Definition (join condition)</b>
 *
 * <ol>
 *   <li>A primitive bool expr
 *   <li>A binary expression
 *   <li>Both operands are column ref
 * </ol>
 */
public class JoinConditionResolver implements Resolver, SQLVisitor {
  @Override
  public boolean enterBinary(SQLNode binary) {
    final SQLExpr.BinaryOp op = binary.get(BINARY_OP);
    final SQLNode left = binary.get(BINARY_LEFT);
    final SQLNode right = binary.get(BINARY_RIGHT);

    if (op == SQLExpr.BinaryOp.EQUAL && isColumn(left) && isColumn(right))
      binary.get(BOOL_EXPR).setJoinCondtion(true);

    return false;
  }

  private static boolean isColumn(SQLNode node) {
    final ColumnRef ref = node.get(RESOLVED_COLUMN_REF);
    assert ref == null || ref.refColumn() != null || ref.refItem() != null;
    return ref != null;
  }

  @Override
  public boolean resolve(Statement stmt) {
    stmt.parsed().accept(this);
    return true;
  }

  private static final Set<Class<? extends Resolver>> DEPENDENCIES =
      Set.of(BoolExprResolver.class, ColumnResolver.class);

  @Override
  public Set<Class<? extends Resolver>> dependsOn() {
    return DEPENDENCIES;
  }

  private static final JoinConditionResolver INSTANCE = new JoinConditionResolver();

  public static JoinConditionResolver singleton() {
    return INSTANCE;
  }
}
