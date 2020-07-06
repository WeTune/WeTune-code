package sjtu.ipads.wtune.systhesis.operators;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.analyzer.ColumnRefCollector;
import sjtu.ipads.wtune.stmt.analyzer.NodeFinder;
import sjtu.ipads.wtune.stmt.attrs.BoolExpr;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;

import java.util.List;

import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.*;

/**
 * Replace predicate by a peer.
 *
 * <p>Requirements:
 *
 * <ol>
 *   <li>the original one and the peer should be both primitive predicate
 *   <li>they should contain columns of the same number
 *   <li>they shouldn't involves a subquery
 * </ol>
 */
public class DisplacePredicate implements Operator {
  private final SQLNode target;
  private final SQLNode replacement;

  private DisplacePredicate(SQLNode target, SQLNode replacement) {
    this.target = target;
    this.replacement = replacement;
  }

  public static DisplacePredicate build(SQLNode target, SQLNode replacement) {
    // we use assertion here because it should be checked in Mutation class
    assert target != null && replacement != null;
    final BoolExpr boolExpr0 = target.get(BOOL_EXPR);
    final BoolExpr boolExpr1 = replacement.get(BOOL_EXPR);
    assert boolExpr0 != null && boolExpr1 != null;
    assert boolExpr0.isPrimitive() && boolExpr1.isPrimitive();

    return new DisplacePredicate(target, replacement);
  }

  @Override
  public SQLNode apply(SQLNode root) {
    final QueryScope scope = target.get(RESOLVED_QUERY_SCOPE);
    final QueryScope.Clause clause = target.get(RESOLVED_CLAUSE_SCOPE);

    final List<SQLNode> originalRefs = ColumnRefCollector.collect(target);
    final List<SQLNode> repRefs = ColumnRefCollector.collect(replacement);
    assert originalRefs.size() == repRefs.size();

    // replace column ref in replacement by original ones
    for (int i = 0; i < originalRefs.size(); i++) {
      final SQLNode originalRef = originalRefs.get(i);
      final SQLNode repRef = repRefs.get(i);

      repRef.replaceThis(originalRef);
    }

    // replace the target with replacement
    target.replaceThis(replacement);

    scope.setScope(target);
    clause.setClause(target);

    return root;
  }
}
