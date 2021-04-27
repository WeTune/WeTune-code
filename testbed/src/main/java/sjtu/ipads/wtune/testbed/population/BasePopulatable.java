package sjtu.ipads.wtune.testbed.population;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

import java.util.List;
import java.util.Objects;
import sjtu.ipads.wtune.testbed.common.BatchActuator;
import sjtu.ipads.wtune.testbed.common.Collection;

class BasePopulatable implements Populatable {
  private final Collection collection;
  private List<Generator> generators;

  private int nextRowId;

  BasePopulatable(Collection collection) {
    this.collection = collection;
  }

  @Override
  public boolean bindGen(Generators generators) {
    this.generators = listMap(generators::bind, collection.elements());
    return this.generators.stream().allMatch(Objects::nonNull);
  }

  @Override
  public boolean populateOne(BatchActuator actuator) {
    actuator.beginOne(collection);
    for (Generator generator : generators) generator.generate(nextRowId, actuator);
    actuator.endOne();

    ++nextRowId;
    return true;
  }
}
