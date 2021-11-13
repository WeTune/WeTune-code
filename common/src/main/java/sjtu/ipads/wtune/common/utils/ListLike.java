package sjtu.ipads.wtune.common.utils;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;

public interface ListLike<T> extends Iterable<T> {
  T get(int index);

  int size();

  default boolean isEmpty() {
    return size() == 0;
  }

  default List<? extends T> asList() {
    return new ListLikeWrapper<>(this);
  }

  @NotNull
  @Override
  default Iterator<T> iterator() {
    return new ListLikeIterator<>(this);
  }
}

class ListLikeWrapper<T> extends AbstractList<T> implements List<T> {
  private final ListLike<T> listLike;

  ListLikeWrapper(ListLike<T> listLike) {
    this.listLike = listLike;
  }

  @Override
  public T get(int index) {
    return listLike.get(index);
  }

  @Override
  public int size() {
    return listLike.size();
  }
}

class ListLikeIterator<T> implements Iterator<T> {
  private final ListLike<T> listLike;
  private int nextIndex;

  ListLikeIterator(ListLike<T> listLike) {
    this.listLike = listLike;
  }

  @Override
  public boolean hasNext() {
    return nextIndex < listLike.size();
  }

  @Override
  public T next() {
    final T ret = listLike.get(nextIndex);
    ++nextIndex;
    return ret;
  }
}
