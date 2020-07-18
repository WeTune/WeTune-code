package sjtu.ipads.wtune.stmt.scriptgen;

import sjtu.ipads.wtune.sqlparser.SQLFormatter;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.attrs.Param;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_PARAM;

public class ParameterizedSQLFormatter extends SQLFormatter {
  private final List<Param> params = new ArrayList<>();

  public ParameterizedSQLFormatter() {
    super();
  }

  public ParameterizedSQLFormatter(boolean oneLine) {
    super(oneLine);
  }

  @Override
  protected SQLFormatter append(String s) {
    builder.append(s.replace("%", "%%"));
    return this;
  }

  @Override
  protected SQLFormatter append(Object obj) {
    builder.append(String.valueOf(obj).replace("%", "%%"));
    return this;
  }

  @Override
  protected SQLFormatter append(char c) {
    if (c == '%') builder.append("%%");
    else builder.append(c);
    return this;
  }

  @Override
  public boolean enterParamMarker(SQLNode paramMarker) {
    final Param param = paramMarker.get(RESOLVED_PARAM);
    if (param != null) {
      builder.append("%s");
      params.add(param);
      return false;
    } else return super.enterParamMarker(paramMarker);
  }

  @Override
  public boolean enterLiteral(SQLNode literal) {
    final Param param = literal.get(RESOLVED_PARAM);
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
