package sjtu.ipads.wtune.symsolver;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.utils.Indexed;

import static org.junit.jupiter.api.Assertions.*;
import static sjtu.ipads.wtune.common.utils.FuncUtils.generate;

public class IndexedTest {
  @Test
  void test() {
    final TableSym[] tbls = generate(3, TableSym.class, i -> TableSym.from(null, i));
    final PickSym[] picks = generate(3, PickSym.class, i -> PickSym.from(null, i));

    Indexed.number(tbls, 0);
    Indexed.number(picks, 1);

    assertTrue(Indexed.isCanonicalIndexed(tbls));
    assertFalse(Indexed.isCanonicalIndexed(picks));

    assertThrows(IllegalStateException.class, () -> Indexed.number(tbls, 0));
    assertThrows(IllegalStateException.class, () -> Indexed.number(picks, 0));
  }
}
