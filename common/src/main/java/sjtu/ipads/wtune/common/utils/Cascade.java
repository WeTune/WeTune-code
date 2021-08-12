package sjtu.ipads.wtune.common.utils;

public interface Cascade {
  Cascade downstream();

  void setDownstream(Cascade downstream);

  boolean forward();

  void reset();

  default boolean init() {
    return forward() && (downstream() == null || downstream().init());
  }

  default boolean next() {
    if (forward()) return true;
    if (downstream() == null) return false;
    if (downstream().forward()) {
      reset();
      return true;
    }
    return false;
  }
}
