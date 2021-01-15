package sjtu.ipads.wtune.sqlparser.pg;

import org.antlr.v4.runtime.ParserRuleContext;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.pg.internal.PGParser;

import java.util.function.Function;

class PGASTBuilderTest {

  private static final PGASTParser PARSER = new PGASTParser();

  private static class TestHelper {
    private String sql;
    private SQLNode node;
    private final Function<PGParser, ParserRuleContext> rule;

    private TestHelper(Function<PGParser, ParserRuleContext> rule) {
      this.rule = rule;
    }

    private SQLNode sql(String sql) {
      if (sql != null) return (node = PARSER.parse(sql, rule));
      return null;
    }
  }

  @Test
  void testCharacterString() {
    final TestHelper helper = new TestHelper(PGParser::data_type);
    final PGParser.Data_typeContext ctx =
        new PGASTParser().parse0("character varying [][]", PGParser::data_type);

    final SQLDataType dataType = PGASTHelper.parseDataType(ctx);
  }
}
