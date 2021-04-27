package sjtu.ipads.wtune.testbed.population;

import sjtu.ipads.wtune.testbed.common.Element;

public interface Generators {
  Generator bind(Element element);

  PopulationConfig config();

  static Generators make(PopulationConfig config) {
    return new SQLGenerators(config);
  }
}
