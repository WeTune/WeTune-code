package sjtu.ipads.wtune.stmt.mutator;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;

public interface Mutation {
  static ASTNode clean(ASTNode node) {
    return Clean.clean(node);
  }

  static ASTNode normalizeBool(ASTNode node) {
    return NormalizeBool.normalize(node);
  }

  static ASTNode normalizeTuple(ASTNode node) {
    return NormalizeTuple.normalize(node);
  }

  static ASTNode normalizeJoinCondition(ASTNode node) {
    return NormalizeJoinCondition.normalize(node);
  }

  static ASTNode normalizeConstantTable(ASTNode node) {
    return NormalizeConstantTable.normalize(node);
  }

  static ASTNode normalizeParam(ASTNode node) {
    return NormalizeParam.normalize(node);
  }
}
