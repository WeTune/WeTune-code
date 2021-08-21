package sjtu.ipads.wtune.common.utils;

interface NoOpTreeContext extends TreeContext<NoOpTreeContext> {
  @Override
  default NoOpTreeContext dup() {
    throw new IllegalArgumentException();
  }
}
