package sjtu.ipads.wtune.testbed.population;

import sjtu.ipads.wtune.testbed.common.Element;

public interface Generators {
  Generator bind(Element element);

  static Generators makeForSQL(Config config) {
    return new SQLGenerators(config);
  }
}
