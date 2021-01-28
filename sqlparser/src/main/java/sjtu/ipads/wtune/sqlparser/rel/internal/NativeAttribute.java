package sjtu.ipads.wtune.sqlparser.rel.internal;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.rel.Attribute;
import sjtu.ipads.wtune.sqlparser.rel.Relation;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Table;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.func2;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.tableNameOf;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.SIMPLE_SOURCE;
import static sjtu.ipads.wtune.sqlparser.rel.Relation.RELATION;

public class NativeAttribute extends BaseAttribute {
  private final Column column;

  private NativeAttribute(Relation owner, Column column) {
    super(owner, column.name());
    this.column = column;
  }

  public static List<Attribute> tableAttributesOf(SQLNode node) {
    if (!SIMPLE_SOURCE.isInstance(node)) throw new IllegalArgumentException();

    final Relation rel = node.get(RELATION);
    final Table table = node.context().schema().table(tableNameOf(node));
    return listMap(func2(NativeAttribute::new).bind0(rel), table.columns());
  }

  @Override
  public String name() {
    return column.name();
  }

  @Override
  public Attribute reference() {
    return null;
  }

  @Override
  public Column column() {
    return column;
  }

  @Override
  public SQLNode node() {
    return null;
  }
}
