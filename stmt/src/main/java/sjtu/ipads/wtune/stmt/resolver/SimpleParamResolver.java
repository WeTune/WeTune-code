package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.statement.Statement;

import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.PARAM_INDEX;

public class SimpleParamResolver implements SQLVisitor, Resolver {
  private int maxIndex = -1;
  private boolean firstPass = true;

  @Override
  public boolean enterParamMarker(SQLNode paramMarker) {
    final Integer index = paramMarker.get(PARAM_INDEX);
    if (index != null && firstPass) maxIndex = Math.max(maxIndex, index);
    if (index == null && !firstPass) paramMarker.put(PARAM_INDEX, ++maxIndex);

    return false;
  }

  @Override
  public boolean enterLiteral(SQLNode literal) {
    final Integer index = literal.get(PARAM_INDEX);
    if (index != null && firstPass) maxIndex = Math.max(maxIndex, index);
    if (index == null && !firstPass) literal.put(PARAM_INDEX, ++maxIndex);
    return false;
  }

  @Override
  public boolean resolve(Statement stmt, SQLNode node) {
    final SimpleParamResolver resolver = new SimpleParamResolver();
    resolver.firstPass = true;
    node.accept(resolver);
    resolver.firstPass = false;
    node.accept(resolver);
    return true;
  }
}
