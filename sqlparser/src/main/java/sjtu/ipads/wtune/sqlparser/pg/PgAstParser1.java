package sjtu.ipads.wtune.sqlparser.pg;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import sjtu.ipads.wtune.sqlparser.ast1.SqlNode;
import sjtu.ipads.wtune.sqlparser.parser.AstParser;
import sjtu.ipads.wtune.sqlparser.parser.ThrowingErrorListener;
import sjtu.ipads.wtune.sqlparser.pg.internal.PGLexer;
import sjtu.ipads.wtune.sqlparser.pg.internal.PGParser;

import java.util.function.Function;

import static sjtu.ipads.wtune.sqlparser.ast1.SqlNode.PostgreSQL;

public class PgAstParser1 implements AstParser {
  public SqlNode parse(String str, Function<PGParser, ParserRuleContext> rule) {
    final PGLexer lexer = new PGLexer(CharStreams.fromString(str));
    final PGParser parser = new PGParser(new CommonTokenStream(lexer));

    lexer.removeErrorListeners();
    lexer.addErrorListener(ThrowingErrorListener.instance());
    parser.removeErrorListeners();
    parser.addErrorListener(ThrowingErrorListener.instance());

    //    lexer.getInterpreter().clearDFA();
    //    parser.getInterpreter().clearDFA();
    return rule.apply(parser).accept(new PgAstBuilder1());
  }

  @Override
  public SqlNode parse(String string) {
    final SqlNode ast = parse(string, PGParser::statement);
    if (ast == null) return null;
    ast.context().setDbType(PostgreSQL);
    return ast;
  }
}
