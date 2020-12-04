package sjtu.ipads.wtune.solver.core;

import sjtu.ipads.wtune.solver.core.impl.IdImpl;

public interface Id {
  int number();

  void setNumber(int i);

  static Id next() {
    return IdImpl.create();
  }
}
