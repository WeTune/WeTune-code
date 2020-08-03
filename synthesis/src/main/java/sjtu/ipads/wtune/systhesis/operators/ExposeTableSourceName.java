package sjtu.ipads.wtune.systhesis.operators;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.analyzer.ColumnAccessAnalyzer;
import sjtu.ipads.wtune.stmt.attrs.ColumnRef;
import sjtu.ipads.wtune.stmt.attrs.TableSource;

import java.util.Set;

/**
 * Set internal name table source to outer scope
 *
 * <p>e.g.
 *
 * <p>SELECT a.i FROM (SELECT b.i, c.j FROM b, c) a WHERE a.j = 3
 *
 * <p>==> SELECT b.i FROM (SELECT b.i, c.j FROM b,c) a WHERE c.j = 3
 *
 * <p>The operation causes the statement invalid.
 */
public class ExposeTableSourceName implements Operator, SQLVisitor {
  private final TableSource source;

  public ExposeTableSourceName(TableSource source) {
    this.source = source;
  }

  public static ExposeTableSourceName build(TableSource source) {
    assert source.isDerived();
    return new ExposeTableSourceName(source);
  }

  @Override
  public SQLNode apply(SQLNode sqlNode) {
    final Set<ColumnRef> cRefs = ColumnAccessAnalyzer.analyze(sqlNode, source, false);

    for (ColumnRef cRef : cRefs) {
      assert source.equals(cRef.source());
      if (cRef.refItem() == null) continue;
      final String simpleName = cRef.refItem().simpleName();
      if (simpleName != null) cRef.putColumnName(simpleName);
    }

    return sqlNode;
  }
}
