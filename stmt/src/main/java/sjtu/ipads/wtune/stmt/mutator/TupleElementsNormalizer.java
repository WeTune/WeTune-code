package sjtu.ipads.wtune.stmt.mutator;

import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.Collections;
import java.util.List;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;

public class TupleElementsNormalizer implements SQLVisitor, Mutator {
  @Override
  public void leaveArray(SQLNode array) {
    final SQLNode parent = array.parent();
    if (exprKind(parent) == SQLExpr.Kind.BINARY
        && parent.get(BINARY_OP) == SQLExpr.BinaryOp.ARRAY_CONTAINS
        && parent.get(BINARY_RIGHT) == array) {
      final List<SQLNode> elements = array.get(ARRAY_ELEMENTS);
      if (elements.size() == 1 && exprKind(elements.get(0)) == Kind.PARAM_MARKER) return;
      array.put(ARRAY_ELEMENTS, Collections.singletonList(paramMarker()));
      array.relinkAll();
    }
  }

  @Override
  public void leaveTuple(SQLNode tuple) {
    final SQLNode parent = tuple.parent();
    if (exprKind(parent) == Kind.BINARY
        && parent.get(BINARY_OP) == BinaryOp.IN_LIST
        && parent.get(BINARY_RIGHT) == tuple) {
      final List<SQLNode> elements = tuple.get(TUPLE_EXPRS);
      if (elements.size() == 1 && exprKind(elements.get(0)) == Kind.PARAM_MARKER) return;
      tuple.put(TUPLE_EXPRS, Collections.singletonList(paramMarker()));
      tuple.relinkAll();
    }
  }

  @Override
  public void mutate(Statement stmt) {
    stmt.parsed().accept(new TupleElementsNormalizer());
  }
}
