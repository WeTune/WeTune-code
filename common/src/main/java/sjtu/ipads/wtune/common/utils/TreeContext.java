package sjtu.ipads.wtune.common.utils;

public interface TreeContext<C extends TreeContext<C>> {
  C dup();
}
