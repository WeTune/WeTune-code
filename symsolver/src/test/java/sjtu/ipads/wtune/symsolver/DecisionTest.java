package sjtu.ipads.wtune.symsolver;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.DecisionTree;

import static org.junit.jupiter.api.Assertions.*;
import static sjtu.ipads.wtune.common.utils.Commons.asArray;
import static sjtu.ipads.wtune.common.utils.FuncUtils.supplier;
import static sjtu.ipads.wtune.symsolver.DecidableConstraint.*;
import static sjtu.ipads.wtune.symsolver.core.Indexed.number;

public class DecisionTest {
  @Test
  void testBasic() {
    final TableSym[] tables = supplier(TableSym::of).repeat(3, TableSym.class);
    final PickSym[] picks = supplier(PickSym::of).repeat(4, PickSym.class);

    number(tables, 0);
    number(picks, 0);
    picks[0].setVisibleSources(asArray(tables[0], tables[1]));

    final DecidableConstraint[] decision = {
      tableEq(tables[0], tables[1]),
      tableEq(tables[0], tables[2]),
      pickEq(picks[0], picks[1]),
      pickFrom(picks[0], tables[0], tables[1])
    };

    final DecisionTree tree = DecisionTree.from(decision);
    assertEquals(3, tree.choices().length);
    assertEquals(8, tree.total());
    assertArrayEquals(new Decision[] {decision[0], decision[1], decision[2]}, tree.choices());

    for (int i = 7; i >= 0; --i) {
      final boolean isSuccessful = tree.forward();
      assertTrue(isSuccessful);
      assertEquals(i, tree.seed());
      assertEquals(Integer.bitCount(i), tree.decisions().length);
    }

    final boolean isSuccessful = tree.forward();
    assertFalse(isSuccessful);
  }

  @Test
  void testFast() {
    final TableSym[] tables = supplier(TableSym::of).repeat(2, TableSym.class);
    final PickSym[] picks = supplier(PickSym::of).repeat(4, PickSym.class);

    number(tables, 0);
    number(picks, 0);
    picks[0].setVisibleSources(asArray(tables[0], tables[1]));

    final DecisionTree tree =
        DecisionTree.fast(
            2,
            4,
            0,
            tableEq(tables[0], tables[1]),
            pickEq(picks[0], picks[1]),
            pickEq(picks[0], picks[2]),
            pickEq(picks[0], picks[3]),
            pickEq(picks[1], picks[2]),
            pickEq(picks[1], picks[3]),
            pickEq(picks[2], picks[3]),
            pickFrom(picks[0], tables[0]),
            pickFrom(picks[0], tables[0], tables[1]),
            pickFrom(picks[0], tables[1]));

    assertEquals(120, tree.total());

    int i = 0;
    while (tree.forward()) i++;
    assertEquals(120, i);
    assertFalse(tree.forward());
  }
}
