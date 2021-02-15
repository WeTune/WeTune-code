package sjtu.ipads.wtune.symsolver;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.symsolver.core.Indexed;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;

import static org.junit.jupiter.api.Assertions.*;
import static sjtu.ipads.wtune.common.utils.FuncUtils.supplier;

public class IndexedTest {
  @Test
  void test() {
    final TableSym[] tbls = supplier(TableSym::of).repeat(3, TableSym.class);
    final PickSym[] picks = supplier(PickSym::of).repeat(3, PickSym.class);

    Indexed.number(tbls, 0);
    Indexed.number(picks, 1);

    assertTrue(Indexed.isCanonicalIndexed(tbls));
    assertFalse(Indexed.isCanonicalIndexed(picks));

    assertThrows(IllegalStateException.class, () -> Indexed.number(tbls, 0));
    assertThrows(IllegalStateException.class, () -> Indexed.number(picks, 0));
  }
}
