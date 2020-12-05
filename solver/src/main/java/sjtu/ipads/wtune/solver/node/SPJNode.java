package sjtu.ipads.wtune.solver.node;

import sjtu.ipads.wtune.solver.node.impl.SPJNodeImpl;
import sjtu.ipads.wtune.solver.sql.ProjectionItem;
import sjtu.ipads.wtune.solver.sql.expr.Expr;
import sjtu.ipads.wtune.solver.sql.expr.InputRef;

import java.util.List;

public interface SPJNode extends AlgNode {
  List<String> inputAliases();

  List<ProjectionItem> projections();

  List<JoinType> joinTypes();

  List<Expr> joinConditions();

  Expr filters();

  static Builder builder() {
    return SPJNodeImpl.builder();
  }

  interface Builder {
    Builder from(AlgNode node, String alias);

    Builder projection(Expr refs, String alias);

    Builder projections(Expr... refs);

    Builder join(AlgNode node, String alias, JoinType type, InputRef left, InputRef ref);

    Builder filter(Expr predicate);

    Builder forceUnique(boolean forceDistinct);

    SPJNode build();

    SPJNode build(boolean compileExpr);
  }
}
