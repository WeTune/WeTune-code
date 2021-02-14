package sjtu.ipads.wtune.common.utils;

import java.util.AbstractList;
import java.util.List;

/**
 * A view of concatenated lists. If underlying list is modified, `get` and `size` will reflect the
 * modification accordingly, while iterator becomes undefined.
 */
class ConcatenatedList<E> extends AbstractList<E> {
  private final List<List<E>> lists;

  ConcatenatedList(List<List<E>> lists) {
    this.lists = lists;
  }

  @Override
  public E get(int index) {
    for (List<E> list : lists) {
      if (index >= list.size()) index -= list.size();
      else return list.get(index);
    }
    throw new IndexOutOfBoundsException(index);
  }

  @Override
  public int size() {
    return lists.stream().mapToInt(List::size).sum();
  }
}
