package sjtu.ipads.wtune.testbed.population;

import java.lang.System.Logger.Level;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.sqlparser.schema.Table;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.testbed.common.BatchActuator;
import sjtu.ipads.wtune.testbed.common.Collection;
import sjtu.ipads.wtune.testbed.common.Element;

public class SQLPopulator implements Populator {
  private PopulationConfig config;
  private Generators generators;
  private boolean showProgressBar;

  @Override
  public void setConfig(PopulationConfig config) {
    if (this.config == config) return;
    this.config = config;
    this.generators = Generators.make(config);
    this.showProgressBar = config.showProgressBar();
  }

  @Override
  public boolean populate(Collection collection) {
    final Populatable populatable = Populatable.ofCollection(collection);
    if (!populatable.bindGen(generators)) return false;

    final BatchActuator actuator = config.actuatorFactory().make(collection.collectionName());
    final int unitCount = config.unitCountOf(collection.collectionName());

    actuator.begin(collection);
    LOG.log(Level.INFO, "begin: {0}", collection.collectionName());
    final boolean showProgressBar = this.showProgressBar;
    final int progressSeg = unitCount / 20;
    final int progressBlk = progressSeg * 5;

    for (int i = 0; i < unitCount; i++) {
      if (showProgressBar && i > 0)
        if (i % progressBlk == 0) System.out.print(i);
        else if (i % progressSeg == 0) System.out.print('.');

      if (!populatable.populateOne(actuator)) return false;
    }
    actuator.end();

    if (showProgressBar) System.out.println("done");
    LOG.log(Level.INFO, "done: {0}", collection.collectionName());

    return true;
  }

  @Override
  public Generator getGenerator(Element element) {
    return generators.bind(element);
  }

  public static void main(String[] args) {
    final App discourse = App.of("discourse");
    final Schema schema = discourse.schema("base", true);
    final Table table = schema.table("api_keys");

    final SQLPopulationConfig config = new SQLPopulationConfig();
    config.setDefaultUnitCount(100);

    final SQLPopulator populator = new SQLPopulator();
    populator.setConfig(config);

    populator.populate(Collection.ofTable(table));

    final Generator generator = populator.getGenerator(Element.ofColumn(table.column("user_id")));
    generator.locate(53).filter(it -> it < 100).limit(1).forEach(System.out::println);
  }
}
