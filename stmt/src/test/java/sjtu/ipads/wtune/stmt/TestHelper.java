package sjtu.ipads.wtune.stmt;

import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class TestHelper {
  public static Iterable<Statement> fastRecycleIter(List<Statement> stmt) {
    return () -> new FastRecycleStmtIterator(stmt);
  }

  private static class FastRecycleStmtIterator implements Iterator<Statement> {
    private final ListIterator<Statement> listIter;

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
}
