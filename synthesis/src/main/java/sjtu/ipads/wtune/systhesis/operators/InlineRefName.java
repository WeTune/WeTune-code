package sjtu.ipads.wtune.systhesis.operators;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.analyzer.TableAccessAnalyzer;
import sjtu.ipads.wtune.stmt.attrs.ColumnRef;
import sjtu.ipads.wtune.stmt.attrs.TableSource;

import java.util.Set;

public class InlineRefName implements Operator, SQLVisitor {
  private final TableSource source;

  public InlineRefName(TableSource source) {
    this.source = source;
  }

  public static InlineRefName build(TableSource source) {
    assert source.isDerived();
    return new InlineRefName(source);
  }

  @Override
  public SQLNode apply(SQLNode sqlNode) {
    final Set<ColumnRef> cRefs = TableAccessAnalyzer.analyze(sqlNode, source, false);

    for (ColumnRef cRef : cRefs) {
      assert source.equals(cRef.source());
      if (cRef.refItem() == null) continue;
      final String simpleName = cRef.refItem().simpleName();
      if (simpleName != null) cRef.putColumnName(simpleName);
    }

    return sqlNode;
  }
}
