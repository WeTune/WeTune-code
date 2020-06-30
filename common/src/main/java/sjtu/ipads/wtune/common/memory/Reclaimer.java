package sjtu.ipads.wtune.common.memory;

@FunctionalInterface
public interface Reclaimer {
  boolean reclaim();
}
