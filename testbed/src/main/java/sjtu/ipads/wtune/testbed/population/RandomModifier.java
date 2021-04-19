package sjtu.ipads.wtune.testbed.population;

import static sjtu.ipads.wtune.testbed.util.RandomHelper.GLOBAL_SEED;

import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.testbed.util.RandGen;

class RandomModifier implements Modifier {
  private final int baseSeed;
  private final RandGen gen;
  private final Generator nextStep;

  RandomModifier(Column column, RandGen gen, Generator nextStep) {
    this.baseSeed = column.hashCode() + GLOBAL_SEED;
    this.gen = gen;
    this.nextStep = nextStep;
  }

  @Override
  public void modify(int seed, Actuator actuator) {
    nextStep.generate(gen.random(baseSeed + seed), actuator);
  }
}
