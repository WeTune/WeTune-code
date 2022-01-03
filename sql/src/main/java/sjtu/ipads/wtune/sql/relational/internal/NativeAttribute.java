package sjtu.ipads.wtune.sql.relational.internal;

import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.sql.ast.ASTNode;
import sjtu.ipads.wtune.sql.relational.Attribute;
import sjtu.ipads.wtune.sql.relational.Relation;
import sjtu.ipads.wtune.sql.schema.Column;
import sjtu.ipads.wtune.sql.schema.Table;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.func2;
import static sjtu.ipads.wtune.sql.ast.TableSourceFields.tableNameOf;
import static sjtu.ipads.wtune.sql.ast.constants.TableSourceKind.SIMPLE_SOURCE;
import static sjtu.ipads.wtune.sql.relational.Relation.RELATION;

public class NativeAttribute extends BaseAttribute {
  private final Column column;

  private NativeAttribute(Relation owner, Column column) {
    super(owner, column.name());
    this.column = column;
  }

  public static List<Attribute> tableAttributesOf(ASTNode node) {
    if (!SIMPLE_SOURCE.isInstance(node)) throw new IllegalArgumentException();

    final Relation rel = node.get(RELATION);
    final Table table = node.context().schema().table(tableNameOf(node));
    return ListSupport.map((Iterable<Column>) table.columns(), func2(NativeAttribute::new).bind0(rel));
  }

  @Override
  public String name() {
    return column.name();
  }

  @Override
  public Attribute reference(boolean recursive) {
    return this;
  }

  @Override
  public Column column(boolean recursive) {
    return column;
  }

  @Override
  public ASTNode selectItem() {
    return null;
  }
}