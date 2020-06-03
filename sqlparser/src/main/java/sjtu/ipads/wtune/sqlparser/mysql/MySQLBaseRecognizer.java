package sjtu.ipads.wtune.sqlparser.mysql;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;

abstract class MySQLBaseRecognizer extends Parser implements MySQLRecognizerCommon {
  private long serverVersion = 0;
  private int sqlMode = NoMode;

  public MySQLBaseRecognizer(TokenStream input) {
    super(input);
  }

  @Override
  public long serverVersion() {
    return serverVersion;
  }

  @Override
  public int sqlMode() {
    return sqlMode;
  }
}
