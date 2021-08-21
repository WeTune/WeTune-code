package sjtu.ipads.wtune.sqlparser.schema;

import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;

import java.util.List;
import java.util.NoSuchElementException;

import static sjtu.ipads.wtune.common.utils.FuncUtils.*;

public class SchemaSupport {
  public static Constraint findIC(Schema schema, List<Column> columns, ConstraintType type) {
    if (columns.isEmpty()) throw new IllegalArgumentException();

    final String ownerTable = columns.get(0).tableName();
    if (!all(columns, it -> ownerTable.equals(it.tableName()))) return null;

    final Table table = schema.table(ownerTable);
    if (table == null) throw new NoSuchElementException("no such table: " + ownerTable);

    final Iterable<Constraint> constraints = table.constraints(type);
    return find(constraints, it -> it.columns().equals(columns));
  }

  public static Iterable<Constraint> findRelatedIC(
      Schema schema, Column column, ConstraintType type) {
    final String ownerTable = column.tableName();
    final Table table = schema.table(ownerTable);
    if (table == null) throw new NoSuchElementException("no such table: " + ownerTable);
    return lazyFilter(table.constraints(type), it -> it.columns().contains(column));
  }
}
