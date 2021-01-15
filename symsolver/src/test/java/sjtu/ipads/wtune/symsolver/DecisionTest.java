package sjtu.ipads.wtune.symsolver;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.symsolver.core.*;
import sjtu.ipads.wtune.symsolver.logic.Proposition;
import sjtu.ipads.wtune.symsolver.logic.Value;
import sjtu.ipads.wtune.symsolver.search.Decision;
import sjtu.ipads.wtune.symsolver.search.DecisionTree;

import java.util.Set;

import static com.google.common.collect.Sets.powerSet;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.*;
import static sjtu.ipads.wtune.common.utils.Commons.asArray;

public class DecisionTest {
  private static class Query0 extends BaseQueryBuilder {
    @Override
    public int numTables() {
      return 2;
    }

    @Override
    public int numPicks() {
      return 3;
    }

    @Override
    public int numPreds() {
      return 0;
    }

    @Override
    protected void prepare() {
      for (PickSym pick : picks) pick.setVisibleSources(tables);
      picks[0].setViableSources(powerSet(Set.of(tables)));
      picks[1].setViableSources(singleton(singleton(tables[0])));
      picks[2].setViableSources(singleton(singleton(tables[1])));
      picks[1].setJoined(picks[2]);
    }

    @Override
    public Value[] output() {
      return asArray(picks[0].apply(tuples));
    }

    @Override
    public Proposition condition() {
      return picks[1].apply(tuples).equalsTo(picks[2].apply(tuples));
    }
  }

  private static class Query1 extends BaseQueryBuilder {
    @Override
    public int numTables() {
      return 1;
    }

    @Override
    public int numPicks() {
      return 1;
    }

    @Override
    public int numPreds() {
      return 0;
    }

    @Override
    protected void prepare() {
      picks[0].setVisibleSources(tables);
      picks[0].setViableSources(singleton(singleton(tables[0])));
    }

    @Override
    public Value[] output() {
      return asArray(picks[0].apply(tuples));
    }

    @Override
    public Proposition condition() {
      return ctx().makeTautology();
    }
  }

  @Test
  void test() {
    final Query0 q0 = new Query0();
    final Query1 q1 = new Query1();

    final Solver solver = Solver.make(q0, q1);
    final TableSym[] tables = solver.tables();
    final PickSym[] picks = solver.picks();

    final DecidableConstraint[] decision = {
      DecidableConstraint.tableEq(tables[0], tables[1]),
      DecidableConstraint.tableEq(tables[0], tables[2]),
      DecidableConstraint.pickEq(picks[0], picks[1]),
      DecidableConstraint.pickFrom(picks[0], tables[0], tables[1])
    };

    final DecisionTree tree = DecisionTree.from(decision);
    assertEquals(3, tree.choices().length);
    assertArrayEquals(new Decision[] {decision[0], decision[1], decision[2]}, tree.choices());

    for (int i = 7; i >= 0; --i) {
      final boolean isSuccessful = tree.forward();
      assertTrue(isSuccessful);
      assertEquals(i, tree.seed());
      assertEquals(Integer.bitCount(i), tree.decisions().length);
    }

    final boolean isSuccessful = tree.forward();
    assertFalse(isSuccessful);

    solver.close();
  }
}
