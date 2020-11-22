package sjtu.ipads.wtune.superopt.impl;

import java.util.function.Consumer;
import java.util.function.Predicate;

public interface Hole<T> {
  boolean fill(T t);

  void unfill();

  boolean isFilled();

  /**
   * Build a instance of Hole from a callback.
   *
   * <p>`fill` invokes callback(t) `unfill` invokes callback(null)
   *
   * @param callback a callback used to fill the hole.
   */
  static <T> Hole<T> ofSetter(Consumer<T> callback) {
    return new Hole<>() {
      private boolean filled = false;

      @Override
      public boolean fill(T t) {
        if (filled) return false;
        callback.accept(t);
        filled = true;
        return true;
      }

      @Override
      public void unfill() {
        if (!filled) return;
        callback.accept(null);
        filled = false;
      }

      @Override
      public boolean isFilled() {
        return filled;
      }
    };
  }
  /**
   * Build a instance of Hole from a callback.
   *
   * <p>`fill` invokes callback(t) `unfill` invokes callback(null)
   *
   * @param callback a callback used to fill the hole. should return false if modification is
   *     applied.
   */
  static <T> Hole<T> ofConditionalSetter(Predicate<T> callback) {
    return new Hole<>() {
      private boolean filled = false;

      @Override
      public boolean fill(T t) {
        if (filled) return false;
        filled = callback.test(t);
        return filled;
      }

      @Override
      public void unfill() {
        if (!filled) return;
        filled = !callback.test(null);
      }

      @Override
      public boolean isFilled() {
        return filled;
      }
    };
  }
}
