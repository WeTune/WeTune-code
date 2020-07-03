package sjtu.ipads.wtune.stmt.analyzer;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.attrs.ColumnRef;
import sjtu.ipads.wtune.stmt.attrs.TableSource;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_COLUMN_REF;

public class TableAccessAnalyzer implements Analyzer<Set<ColumnRef>>, SQLVisitor {
  private TableSource source;
  private boolean recursive;
  private Set<ColumnRef> refs;

  @Override
  public void setParam(Object... args) {
    source = (TableSource) args[0];
    recursive = args.length >= 2 && (boolean) args[1];
  }

  @Override
  public boolean enterColumnRef(SQLNode columnRef) {
    final ColumnRef cRef = columnRef.get(RESOLVED_COLUMN_REF);
    if (cRef == null) return false;
    if ((recursive && cRef.isFrom(source)) || (!recursive && source.equals(cRef.source())))
      refs.add(cRef);
    return false;
  }

  @Override
  public Set<ColumnRef> analyze(SQLNode node) {
    if (source == null) return Collections.emptySet();
    refs = new HashSet<>();
    node.accept(this);
    return refs;
  }

  public static Set<ColumnRef> analyze(SQLNode node, TableSource source, boolean recursive) {
    final TableAccessAnalyzer analyzer = new TableAccessAnalyzer();
    analyzer.source = source;
    analyzer.recursive = recursive;
    return analyzer.analyze(node);
  }
}
