package sjtu.ipads.wtune.stmt.analyzer;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.stmt.attrs.RelationGraph;
import sjtu.ipads.wtune.stmt.schema.Column;

import java.util.Set;

public class Analysis {
  public static Set<Column> inferForeignKey(SQLNode node) {
    return InferForeignKey.analyze(node);
  }

  public static RelationGraph buildRelationGraph(SQLNode node) {
    return RelationGraphAnalyzer.analyze(node);
  }
}
