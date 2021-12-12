package sjtu.ipads.wtune.common.utils;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Iterator;

class ZippedIterator<X, Y> implements Iterator<Pair<X, Y>> {
  private final Iterator<X> iterX;
  private final Iterator<Y> iterY;

  ZippedIterator(Iterator<X> iterX, Iterator<Y> iterY) {
    this.iterX = iterX;
    this.iterY = iterY;
  }

  @Override
  public boolean hasNext() {
    return iterX.hasNext() && iterY.hasNext();
  }

  @Override
  public Pair<X, Y> next() {
    final X x = iterX.next();
    final Y y = iterY.next();
    return Pair.of(x, y);
  }
}
