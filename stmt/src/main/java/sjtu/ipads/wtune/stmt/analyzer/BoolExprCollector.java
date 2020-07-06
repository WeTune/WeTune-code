package sjtu.ipads.wtune.stmt.analyzer;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.attrs.BoolExpr;
import sjtu.ipads.wtune.stmt.resovler.BoolExprResolver;
import sjtu.ipads.wtune.stmt.resovler.Resolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.BOOL_EXPR;

public class BoolExprCollector implements Analyzer<List<SQLNode>>, SQLVisitor {
  private final List<SQLNode> nodes = new ArrayList<>();
  private Predicate<BoolExpr> filter;

  public static final Predicate<BoolExpr> PRIMITIVE = BoolExpr::isPrimitive;

  @Override
  public boolean enter(SQLNode node) {
    final BoolExpr boolExpr = node.get(BOOL_EXPR);
    if (boolExpr != null && (filter == null || filter.test(boolExpr))) nodes.add(node);
    return true;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void setParam(Object... args) {
    filter = (Predicate<BoolExpr>) args[0];
  }

  @Override
  public List<SQLNode> analyze(SQLNode node) {
    node.accept(this);
    return nodes;
  }

  public static List<SQLNode> collect(SQLNode node) {
    return new BoolExprCollector().analyze(node);
  }

  public static List<SQLNode> collectPrimitive(SQLNode node) {
    final BoolExprCollector collector = new BoolExprCollector();
    collector.filter = PRIMITIVE;
    return collector.analyze(node);
  }

  private static final Set<Class<? extends Resolver>> DEPENDENCIES = Set.of(BoolExprResolver.class);

  @Override
  public Set<Class<? extends Resolver>> dependsOn() {
    return DEPENDENCIES;
  }
}
