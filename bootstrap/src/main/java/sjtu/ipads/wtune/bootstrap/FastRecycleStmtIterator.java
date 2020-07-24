package sjtu.ipads.wtune.bootstrap;

import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

class FastRecycleStmtIterator implements Iterator<Statement> {
  private final ListIterator<Statement> listIter;

  public static Iterable<Statement> fastRecycleIter(List<Statement> stmt) {
    return () -> new FastRecycleStmtIterator(stmt);
  }

  private FastRecycleStmtIterator(List<Statement> stmts) {
    listIter = stmts.listIterator();
  }

  @Override
  public boolean hasNext() {
    return listIter.hasNext();
  }

  @Override
  public Statement next() {
    final Statement next = listIter.next();
    listIter.remove();
    return next;
  }
}
