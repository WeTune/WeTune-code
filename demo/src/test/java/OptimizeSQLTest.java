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
  void testSQL0() {
    final String rawSql = "SELECT `tags`.`id` FROM `tags` INNER JOIN `taggings` ON `tags`.`id` = `taggings`.`tag_id` WHERE `taggings`.`taggable_id` = 1234 AND `taggings`.`taggable_type` = 'Contact' AND `taggings`.`context` = 'tags'";
    final String appName = "fatfreecrm";
    final Schema schema = App.of(appName).schema("base", true);

    final OptimizeStat optRes = OptimizeSQLSupport.optimizeSQL(rawSql, appName, schema, bank);
    assert optRes.isOptimized();
    System.out.println(optRes.optSql());
  }

  @Test
  void testSchema() {
    final String appName = "broadleaf";
    final String tableName = "blc_additional_offer_info";
    final String columnName = "OFFER_ID";
    final Schema schemaPatched = App.of(appName).schema("base", true);
    final Schema schemaRaw = App.of(appName).schema("base", false);

    System.out.println("patched constraints: ");
    schemaPatched.table(tableName).column(columnName).constraints().forEach(System.out::println);
    System.out.println("raw constraints: ");
    schemaRaw.table(tableName).column(columnName).constraints().forEach(System.out::println);
  }

  private static Path dataDir() {
    return Path.of(System.getProperty("wetune.data_dir", "wtune_data"));
  }
}
