package sjtu.ipads.wtune.stmt.scriptgen;

import sjtu.ipads.wtune.sqlparser.ast.Formatter;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.stmt.resolver.Param;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.stmt.resolver.ParamManager.PARAM;

public class ParameterizedSQLFormatter extends Formatter {
  private final List<Param> params = new ArrayList<>();

  public ParameterizedSQLFormatter() {
    super(true);
  }

  @Override
  protected Formatter append(String s) {
    builder.append(s.replace("%", "%%"));
    return this;
  }

  @Override
  protected Formatter append(Object obj) {
    builder.append(String.valueOf(obj).replace("%", "%%"));
    return this;
  }

  @Override
  protected Formatter append(char c) {
    if (c == '%') builder.append("%%");
    else builder.append(c);
    return this;
  }

  @Override
  public boolean enterParamMarker(SQLNode paramMarker) {
    final Param param = paramMarker.get(PARAM);
    if (param != null) {
      builder.append("%s");
      params.add(param);
      return false;
    } else return super.enterParamMarker(paramMarker);
  }

  @Override
  public boolean enterLiteral(SQLNode literal) {
    final Param param = literal.get(PARAM);
    if (param != null) {
      builder.append("%s");
      params.add(param);
      return false;
    } else return super.enterLiteral(literal);
  }

  public List<Param> params() {
    return params;
  }
}
