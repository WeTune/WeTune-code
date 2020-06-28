package sjtu.ipads.wtune.stmt.mutator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLParser;
import sjtu.ipads.wtune.stmt.StatementDao;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CleanerTest {
  private static SQLParser MYSQL_PARSER;

  private static String dbPath() {
    return Paths.get(System.getProperty("user.dir"))
        .getParent()
        .resolve("data/wtune.db")
        .toString();
  }

  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    StatementDao.fromDb(StatementDao.connectionSupplier("jdbc:sqlite://" + dbPath()))
        .registerAsGlobal();
    MYSQL_PARSER = SQLParser.mysql();
  }

  @Test
  @DisplayName("[stmt.mutator.cleaner] simple statements")
  void testSimple() {
    SQLNode node;
    {
      node = MYSQL_PARSER.parse("select a from t where 1=1");
      node = Cleaner.doMutate(node);
      assertEquals("SELECT `a` FROM `t`", node.toString());
    }
    {
      node = MYSQL_PARSER.parse("select a from t where true and b");
      node = Cleaner.doMutate(node);
      assertEquals("SELECT `a` FROM `t` WHERE `b`", node.toString());
    }
    {
      node = MYSQL_PARSER.parse("select a from t where 1+1 = 2 and (c or 1 between 0 and 2)");
      node = Cleaner.doMutate(node);
      assertEquals("SELECT `a` FROM `t` WHERE `c`", node.toString());
    }
    {
      node =
          MYSQL_PARSER.parse(
              "select a from t where now() = 0 or rand() = 10 or field(k, 1, 2) = 3");
      node = Cleaner.doMutate(node);
      assertEquals(
          "SELECT `a` FROM `t` WHERE RAND() = 10 OR FIELD(`k`, 1, 2) = 3", node.toString());
    }
  }

  private static final Set<String> PG_APPS =
      new HashSet<>(Arrays.asList("gitlab", "discourse", "homeland"));

  @Test
  @DisplayName("[stmt.mutator.cleaner] all statements")
  void testAll() {
    final List<Statement> stmts = Statement.findAll();
    for (Statement stmt : stmts) {
      if (PG_APPS.contains(stmt.appName())) continue;

      SQLNode node = MYSQL_PARSER.parse(stmt.rawSql());
      final String original = node.toString();
      node = Cleaner.doMutate(node);
      final String modified = node.toString();
      assertFalse(modified.contains("<??>"));
      assertTrue(!original.contains("1 = 1") || !modified.contains("1 = 1"));
      assertTrue(!original.contains("1 = 0") || !modified.contains("1 = 0"));
    }
  }
}
