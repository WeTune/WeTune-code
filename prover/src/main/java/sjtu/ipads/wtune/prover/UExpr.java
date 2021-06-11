package sjtu.ipads.wtune.prover;

import java.util.Set;

public interface UExpr {
  enum Kind {
    TABLE_FUNC,
  }

  Kind kind();

  Set<Tuple> tuple();

  void replace(Tuple v1, Tuple v2);
}
