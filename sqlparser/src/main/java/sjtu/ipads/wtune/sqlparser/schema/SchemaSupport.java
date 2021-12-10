package sjtu.ipads.wtune.sqlparser.schema;

import sjtu.ipads.wtune.common.utils.IterableSupport;
import sjtu.ipads.wtune.sqlparser.ast1.constants.ConstraintKind;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static sjtu.ipads.wtune.common.utils.FuncUtils.*;

public class SchemaSupport {
  public static List<Constraint> findIC(Schema schema, List<Column> columns, ConstraintKind type) {
    if (columns.isEmpty()) return Collections.emptyList();

    final String ownerTable = columns.get(0).tableName();
    if (!IterableSupport.all(columns, it -> ownerTable.equals(it.tableName()))) return Collections.emptyList();

    final Table table = schema.table(ownerTable);
    if (table == null) throw new NoSuchElementException("no such table: " + ownerTable);

    final Iterable<Constraint> constraints = table.constraints(type);
    return listFilter(constraints, it -> it.columns().equals(columns));
  }

  public static Iterable<Constraint> findRelatedIC(
      Schema schema, Column column, ConstraintKind type) {
    final String ownerTable = column.tableName();
    final Table table = schema.table(ownerTable);
    if (table == null) throw new NoSuchElementException("no such table: " + ownerTable);
    return IterableSupport.lazyFilter(table.constraints(type), it -> it.columns().contains(column));
  }
}
