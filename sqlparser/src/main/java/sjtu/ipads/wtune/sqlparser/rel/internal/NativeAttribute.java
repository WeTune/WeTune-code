package sjtu.ipads.wtune.sqlparser.rel.internal;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.rel.Attribute;
import sjtu.ipads.wtune.sqlparser.rel.Column;
import sjtu.ipads.wtune.sqlparser.rel.Relation;
import sjtu.ipads.wtune.sqlparser.rel.Table;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceAttrs.tableNameOf;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.SIMPLE_SOURCE;

public class NativeAttribute extends BaseAttribute {
  private final Column column;

  private NativeAttribute(Relation owner, Column column) {
    super(owner, column.name());
    this.column = column;
  }

  public static List<Attribute> tableAttributesOf(SQLNode node) {
    if (!SIMPLE_SOURCE.isInstance(node)) throw new IllegalArgumentException();

    final Relation rel = Relation.of(node);
    final Table table = node.context().schema().table(tableNameOf(node));
    return listMap(it -> new NativeAttribute(rel, it), table.columns());
  }

  @Override
  public String name() {
    return column.name();
  }

  @Override
  public SQLNode node() {
    return null;
  }

  @Override
  public Column column() {
    return column;
  }
}
