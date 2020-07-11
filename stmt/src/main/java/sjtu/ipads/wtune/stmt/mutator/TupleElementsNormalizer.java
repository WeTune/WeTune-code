package sjtu.ipads.wtune.stmt.mutator;

import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.Collections;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;

public class TupleElementsNormalizer implements SQLVisitor, Mutator {
  @Override
  public void leaveArray(SQLNode array) {
    final SQLNode parent = array.parent();
    if (exprKind(parent) == SQLExpr.Kind.BINARY
        && parent.get(BINARY_OP) == SQLExpr.BinaryOp.ARRAY_CONTAINS
        && parent.get(BINARY_RIGHT) == array) {
      array.put(ARRAY_ELEMENTS, Collections.singletonList(paramMarker()));
    }
  }

  @Override
  public void leaveTuple(SQLNode tuple) {
    final SQLNode parent = tuple.parent();
    if (exprKind(parent) == Kind.BINARY
        && parent.get(BINARY_OP) == BinaryOp.IN_LIST
        && parent.get(BINARY_RIGHT) == tuple) {
      tuple.put(TUPLE_EXPRS, Collections.singletonList(paramMarker()));
    }
  }

  @Override
  public void mutate(Statement stmt) {
    stmt.parsed().accept(new TupleElementsNormalizer());
    stmt.parsed().relinkAll();
  }
}
