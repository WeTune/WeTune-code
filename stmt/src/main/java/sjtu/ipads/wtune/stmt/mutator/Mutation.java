package sjtu.ipads.wtune.stmt.mutator;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;

public interface Mutation {
  static SQLNode clean(SQLNode node) {
    return Clean.clean(node);
  }

  static SQLNode normalizeBool(SQLNode node) {
    return NormalizeBool.normalize(node);
  }

  static SQLNode normalizeTuple(SQLNode node) {
    return NormalizeTuple.normalize(node);
  }

  static SQLNode normalizeConstantTable(SQLNode node) {
    return NormalizeConstantTable.normalize(node);
  }

  static SQLNode normalizeParam(SQLNode node) {
    return NormalizeParam.normalize(node);
  }
}
