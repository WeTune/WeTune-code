import org.junit.jupiter.api.Test;
import wtune.demo.optimize.OptimizeSQLSupport;
import wtune.demo.optimize.OptimizeStat;
import wtune.sql.schema.Schema;
import wtune.stmt.App;
import wtune.superopt.substitution.SubstitutionBank;
import wtune.superopt.substitution.SubstitutionSupport;

import java.io.IOException;
import java.nio.file.Path;

public class OptimizeSQLTest {

  private static SubstitutionBank bank;

  static {
    try {
      bank = SubstitutionSupport.loadBank(dataDir().resolve("prepared/rules.txt"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  void testOptimizeSQL0() {
    final String rawSql = "SELECT `tags`.`id` FROM `tags` INNER JOIN `taggings` ON `tags`.`id` = `taggings`.`tag_id` WHERE `taggings`.`taggable_id` = 1234 AND `taggings`.`taggable_type` = 'Contact' AND `taggings`.`context` = 'tags'";
    final String appName = "fatfreecrm";
    final Schema schema = App.of(appName).schema("base", true);

    final OptimizeStat optRes = OptimizeSQLSupport.optimizeSQL(rawSql, schema, bank);
    assert optRes.isOptimized();
    System.out.println(optRes.optSqls());
    System.out.println(optRes.ruleSteps());
  }

  @Test
  void testOptimizeSQLToMinCost0() {
    final String rawSql = "SELECT `tags`.`id` FROM `tags` INNER JOIN `taggings` ON `tags`.`id` = `taggings`.`tag_id` WHERE `taggings`.`taggable_id` = 1234 AND `taggings`.`taggable_type` = 'Contact' AND `taggings`.`context` = 'tags'";
    final String appName = "fatfreecrm";
    final Schema schema = App.of(appName).schema("base", true);

    final OptimizeStat optRes = OptimizeSQLSupport.optimizeSQLToMinCost(rawSql, appName, schema, bank);
    assert optRes.isOptimized();
    System.out.println(optRes.optSqls());
  }

  private static Path dataDir() {
    return Path.of(System.getProperty("wetune.data_dir", "wtune_data"));
  }
}
