package sjtu.ipads.wtune.testbed.population;

import java.lang.System.Logger;
import sjtu.ipads.wtune.testbed.common.Collection;

public interface Populator {
  Logger LOG = System.getLogger("Populator");

  void setConfig(Config config);

  boolean populate(Collection collection);
}
