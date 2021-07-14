package sjtu.ipads.wtune.prover.uexpr;

import java.util.Collections;
import java.util.List;

public interface UTerm extends UExpr {
  @Override
  default List<UExpr> children() {
    return Collections.emptyList();
  }
}
