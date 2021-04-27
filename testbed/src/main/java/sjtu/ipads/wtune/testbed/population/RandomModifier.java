package sjtu.ipads.wtune.testbed.population;

import java.util.stream.IntStream;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.testbed.common.BatchActuator;
import sjtu.ipads.wtune.testbed.util.RandGen;

class RandomModifier implements Modifier {
  private final int localSeed; // to make the value seq different in different columns
  private final RandGen gen;
  private final Generator nextStep;

  RandomModifier(Column column, RandGen gen, Generator nextStep) {
    this.localSeed = column.hashCode() & ((1 << 30) - 1);
    this.gen = gen;
    this.nextStep = nextStep;
  }

  @Override
  public void modify(int seed, BatchActuator actuator) {
    if (seed < 0) throw new IllegalArgumentException("seed is out of bound " + seed);

    nextStep.generate(gen.random(localSeed ^ seed), actuator);
  }

  @Override
  public IntStream locate(Object value) {
    final int localSeed = this.localSeed;
    return nextStep.locate(value).map(it -> gen.reverse(it) ^ localSeed);
  }
}
