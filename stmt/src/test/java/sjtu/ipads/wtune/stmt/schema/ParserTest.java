package sjtu.ipads.wtune.stmt.schema;

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
  private static StatementDao DAO;
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
    DAO = StatementDao.fromDb(StatementDao.connectionSupplier("jdbc:sqlite://" + dbPath()));
    MYSQL_PARSER = SQLParser.mysql();
  }

  @AfterAll
  static void toreDown() {
    DAO.close();
  }

  private static final Set<String> PG_APPS =
      new HashSet<>(Arrays.asList("gitlab", "discourse", "homeland"));

  @Test
  @DisplayName("parser capability")
  void test() {
    final List<Statement> stmts = DAO.findAll();

    for (Statement stmt : stmts) {
      if (PG_APPS.contains(stmt.appName())) continue; // TODO
      final SQLNode parsed = MYSQL_PARSER.parse(stmt.rawSql());
      assertFalse(parsed.toString().contains("<??>"));
    }
  }
}
