package sjtu.ipads.wtune.sqlparser.rel;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.rel.internal.ProjectionAttribute;
import sjtu.ipads.wtune.sqlparser.rel.internal.ColumnAttribute;

public interface Attribute {
  String name();

  static Attribute fromColumn(Column column) {
    return ColumnAttribute.build(column);
  }

  static Attribute fromProjection(SQLNode selectItem){
    return ProjectionAttribute.build(selectItem);
  }
}
