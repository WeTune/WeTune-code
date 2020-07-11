package sjtu.ipads.wtune.stmt.analyzer;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.resolver.IdResolver;
import sjtu.ipads.wtune.stmt.resolver.Resolver;

import java.util.Set;

import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.NODE_ID;

public class NodeFinder implements Analyzer<SQLNode>, SQLVisitor {
  private final Long targetId;
  private SQLNode found = null;

  public NodeFinder(Long targetId) {
    this.targetId = targetId;
  }

  @Override
  public boolean enter(SQLNode node) {
    if (found != null) return false;
    if (targetId.equals(node.get(NODE_ID))) {
      found = node;
      return false;
    }
    return true;
  }

  @Override
  public SQLNode analyze(SQLNode node) {
    if (targetId == null) return null;
    node.accept(this);
    return found;
  }

  public static SQLNode find(SQLNode root, Long id) {
    return new NodeFinder(id).analyze(root);
  }

  public static SQLNode find(SQLNode root, SQLNode target) {
    if (target == null) return null;
    return find(root, target.get(NODE_ID));
  }

  private static final Set<Class<? extends Resolver>> DEPENDENCIES = Set.of(IdResolver.class);

  @Override
  public Set<Class<? extends Resolver>> dependsOn() {
    return DEPENDENCIES;
  }
}
