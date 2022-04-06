import org.junit.jupiter.api.Test;
import wtune.common.datasource.DbSupport;
import wtune.demo.optimize.OptimizeSQLSupport;
import wtune.demo.optimize.OptimizeStat;
import wtune.sql.SqlSupport;
import wtune.sql.schema.Schema;
import wtune.sql.schema.SchemaSupport;
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
    final String rawSql =
        "SELECT `tags`.`id` FROM `tags` INNER JOIN `taggings` ON `tags`.`id` = `taggings`.`tag_id` WHERE `taggings`.`taggable_id` = 1234 AND `taggings`.`taggable_type` = 'Contact' AND `taggings`.`context` = 'tags'";
    final String appName = "fatfreecrm";
    final Schema schema = App.of(appName).schema("base", true);

    final OptimizeStat optRes =
        OptimizeSQLSupport.optimizeSQL(rawSql, DbSupport.MySQL, schema, bank);
    assert optRes.isOptimized();
    System.out.println(optRes.optSqls());
    System.out.println(optRes.ruleSteps());
  }

  @Test
  void testOptimizeSQLWithoutSchema0() {
    final String rawSql =
        "SELECT t1.`id` FROM `tags` as t1 LEFT JOIN `tags` as t2 ON t1.`id` = t2.`id` WHERE t1.name = 'abc'";
    final OptimizeStat optRes = OptimizeSQLSupport.optimizeSQL(rawSql, DbSupport.MySQL, null, bank);
    assert optRes.isOptimized();
    System.out.println(optRes.optSqls());
  }

  @Test
  void testOptimizeSQLWithoutSchema1() {
    final String rawSql =
        "SELECT t1.`id` FROM `tags` AS t1  WHERE t1.`tid` IN (SELECT t2.`tid` FROM `tags` AS t2)";
    final OptimizeStat optRes = OptimizeSQLSupport.optimizeSQL(rawSql, DbSupport.MySQL, null, bank);
    assert optRes.isOptimized();
    System.out.println(optRes.optSqls());
  }

  @Test
  void testSchemaAutoDetect0() {
    final String rawSql =
        "SELECT t1.`id` FROM `tags` as t1 LEFT JOIN `tags` as t2 ON t1.`id` = t2.`id` WHERE t1.name = 'abc'";
    final Schema schema =
        SchemaSupport.parseSimpleSchema(
            DbSupport.MySQL, SqlSupport.parseSql(DbSupport.MySQL, rawSql));

    assert schema != null;
    System.out.println(schema.tables());
  }

  @Test
  void testSchemaAutoDetect1() {
    final String rawSql =
        "SELECT t1.`id` FROM `tags` AS t1  WHERE t1.`tid` IN (SELECT t2.`tid` FROM `tags` AS t2)";
    final Schema schema =
        SchemaSupport.parseSimpleSchema(
            DbSupport.MySQL, SqlSupport.parseSql(DbSupport.MySQL, rawSql));

    assert schema != null;
    System.out.println(schema.tables());
  }

  private static Path dataDir() {
    return Path.of(System.getProperty("wetune.data_dir", "wtune_data"));
  }
}
