package sjtu.ipads.wtune.testbed.population;

import java.lang.System.Logger.Level;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.sqlparser.schema.Table;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.testbed.common.Collection;

public class SQLPopulator implements Populator {
  private Config config;
  private Generators generators;
  private boolean showProgressBar;

  @Override
  public void setConfig(Config config) {
    if (this.config == config) return;
    this.config = config;
    this.generators = Generators.makeForSQL(config);
    this.showProgressBar = config.showProgressBar();
  }

  @Override
  public boolean populate(Collection collection) {
    final Populatable populatable = Populatable.ofCollection(collection);
    if (!populatable.bindGen(generators)) return false;

    final Actuator actuator = config.getActuatorFactory().get();
    final int unitCount = config.getUnitCount(collection.collectionName());

    actuator.begin(collection);
    LOG.log(Level.INFO, "begin: {0}", collection.collectionName());
    final boolean showProgressBar = this.showProgressBar;

    for (int i = 0; i < unitCount; i++) {
      if (showProgressBar && i > 0)
        if (i % 10000 == 0) System.out.print(i);
        else if (i % 2000 == 0) System.out.print('.');

      if (!populatable.populateOne(actuator)) return false;
    }
    actuator.end();

    if (showProgressBar) System.out.println("done");
    LOG.log(Level.INFO, "done: {0}", collection.collectionName());

    return true;
  }

  public static void main(String[] args) {
    final App discourse = App.of("discourse");
    final Schema schema = discourse.schema("base", true);
    final Table tablePosts = schema.table("topic_allowed_groups");

    final SQLConfig config = new SQLConfig();
    config.setDefaultUnitCount(100);

    final SQLPopulator populator = new SQLPopulator();
    populator.setConfig(config);

    populator.populate(Collection.ofTable(tablePosts));
  }
}
