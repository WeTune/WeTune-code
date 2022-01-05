package sjtu.ipads.wtune.testbed.population;

import sjtu.ipads.wtune.testbed.common.Collection;
import sjtu.ipads.wtune.testbed.common.Element;

import java.lang.System.Logger;

public interface Populator {
  Logger LOG = System.getLogger("Populator");

  void setConfig(PopulationConfig config);

  boolean populate(Collection collection);

  Generator getGenerator(Element element);
}
