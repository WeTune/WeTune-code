package sjtu.ipads.wtune.testbed.population;

import java.lang.System.Logger;
import sjtu.ipads.wtune.testbed.common.Collection;
import sjtu.ipads.wtune.testbed.common.Element;

public interface Populator {
  Logger LOG = System.getLogger("Populator");

  void setConfig(PopulationConfig config);

  boolean populate(Collection collection);

  Generator getGenerator(Element element);
}
