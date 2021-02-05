package sjtu.ipads.wtune.superopt.optimization;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

public interface FilterOp extends Operator {
  ASTNode expr();
}
