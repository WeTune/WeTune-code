package sjtu.ipads.wtune.systhesis.operators;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.attrs.ColumnRef;
import sjtu.ipads.wtune.stmt.attrs.TableSource;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.SQLNode.COLUMN_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.DERIVED_ALIAS;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.SIMPLE_ALIAS;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_COLUMN_REF;
import static sjtu.ipads.wtune.stmt.utils.StmtHelper.nodeEquals;

public class RenameTableSource implements Operator, SQLVisitor {
  private final TableSource source;
  private final String name;

  private RenameTableSource(TableSource source, String name) {
    this.source = source;
    this.name = name;
  }

  public static RenameTableSource build(TableSource source, String name) {
    if (source == null || name == null) return null;
    return new RenameTableSource(source, name);
  }

  @Override
  public boolean enterColumnRef(SQLNode columnRef) {
    final ColumnRef cRef = columnRef.get(RESOLVED_COLUMN_REF);
    if (cRef == null) return false;
    if (source.equals(cRef.source()))
      cRef.node().get(COLUMN_REF_COLUMN).put(COLUMN_NAME_TABLE, name);
    return false;
  }

  @Override
  public boolean enterSimpleTableSource(SQLNode simpleTableSource) {
    if (nodeEquals(simpleTableSource, source.node())) simpleTableSource.put(SIMPLE_ALIAS, name);
    return false;
  }

  @Override
  public boolean enterDerivedTableSource(SQLNode derivedTableSource) {
    if (nodeEquals(derivedTableSource, source.node())) derivedTableSource.put(DERIVED_ALIAS, name);
    return false;
  }

  @Override
  public SQLNode apply(SQLNode sqlNode) {
    sqlNode.accept(this);
    return sqlNode;
  }
}
