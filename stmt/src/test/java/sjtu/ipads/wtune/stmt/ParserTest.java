package sjtu.ipads.wtune.stmt;

import org.junit.jupiter.api.AfterAll;
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

import static org.junit.jupiter.api.Assertions.assertFalse;

public class ParserTest {
  private static SQLParser MYSQL_PARSER;

  @BeforeAll
  static void setUp() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    StatementDao.fromDb(StatementDao.connectionSupplier("jdbc:sqlite://" + dbPath()))
        .registerAsGlobal();
    MYSQL_PARSER = SQLParser.mysql();
  }

  @AfterAll
  static void toreDown() {
    StatementDao.getGlobal().close();
  }

  private static String dbPath() {
    return Paths.get(System.getProperty("user.dir"))
        .getParent()
        .resolve("data/wtune.db")
        .toString();
  }

  private static final Set<String> PG_APPS =
      new HashSet<>(Arrays.asList("gitlab", "discourse", "homeland"));

  @Test
  @DisplayName("[stmt] parsing all statements")
  void test() {
    final List<Statement> stmts = Statement.findAll();

    for (Statement stmt : stmts) {
      if (PG_APPS.contains(stmt.appName())) continue; // TODO
      final SQLNode parsed = MYSQL_PARSER.parse(stmt.rawSql());
      assertFalse(parsed.toString().contains("<??>"));
    }
  }
}
