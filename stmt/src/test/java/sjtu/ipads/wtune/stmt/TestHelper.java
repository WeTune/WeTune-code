package sjtu.ipads.wtune.stmt;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class TestHelper {
  public static <T> Iterable<T> fastRecycleIter(List<T> stmt) {
    return () -> new FastRecycleStmtIterator<>(stmt);
  }

  private static class FastRecycleStmtIterator<T> implements Iterator<T> {
    private final ListIterator<T> listIter;

    private FastRecycleStmtIterator(List<T> stmts) {
      listIter = stmts.listIterator();
    }

    @Override
    public boolean hasNext() {
      return listIter.hasNext();
    }

    @Override
    public T next() {
      final T next = listIter.next();
      listIter.remove();
      return next;
    }
  }
}
