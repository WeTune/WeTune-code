package sjtu.ipads.wtune.stmt.analyzer;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.attrs.ColumnRef;
import sjtu.ipads.wtune.stmt.resovler.ColumnResolver;
import sjtu.ipads.wtune.stmt.resovler.Resolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_COLUMN_REF;

public class ColumnRefCollector implements Analyzer<List<SQLNode>>, SQLVisitor {
  private final List<SQLNode> refs = new ArrayList<>();

  @Override
  public boolean enterColumnRef(SQLNode columnRef) {
    if (columnRef.get(RESOLVED_COLUMN_REF) != null) refs.add(columnRef);
    return false;
  }

  @Override
  public List<SQLNode> analyze(SQLNode node) {
    node.accept(this);
    return refs;
  }

  public static List<SQLNode> collect(SQLNode node) {
    return new ColumnRefCollector().analyze(node);
  }

  private static final Set<Class<? extends Resolver>> DEPENDENCIES = Set.of(ColumnResolver.class);

  @Override
  public Set<Class<? extends Resolver>> dependsOn() {
    return DEPENDENCIES;
  }
}
