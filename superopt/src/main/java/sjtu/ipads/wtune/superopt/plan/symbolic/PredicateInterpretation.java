package sjtu.ipads.wtune.superopt.plan.symbolic;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

import java.util.List;

public interface PredicateInterpretation {
  ASTNode instantiate(List<ASTNode> picks);
}
