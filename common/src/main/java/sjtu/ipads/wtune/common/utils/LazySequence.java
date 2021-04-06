package sjtu.ipads.wtune.common.utils;

import java.util.Iterator;

public interface LazySequence<T> {
  /**
   * Generate the next element.
   *
   * <p>Only if the immediately following {@link #isDrained()} returns true, the returned value is
   * valid. If there is no more elements, the returned value is undefined. The method never throws
   * NoSuchElementException.
   */
  T next();

  /**
   * Indicate whether there is no more element in the sequence.
   *
   * <p>Only if this method returns true, the returned value of last {@link #next()} is valid.
   */
  boolean isDrained();

  static <T> LazySequence<T> fromIterator(Iterator<T> iter) {
    return new LazySequence<>() {
      private boolean isDrained = false;

      @Override
      public T next() {
        if (iter.hasNext()) return iter.next();
        else {
          isDrained = true;
          return null;
        }
      }

      @Override
      public boolean isDrained() {
        return isDrained;
      }
    };
  }
}
