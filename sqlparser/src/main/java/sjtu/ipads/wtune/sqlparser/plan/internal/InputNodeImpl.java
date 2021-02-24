package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.InputNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanAttribute;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.relational.Relation;
import sjtu.ipads.wtune.sqlparser.schema.Table;

import java.util.Collections;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.node;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.TABLE_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.SIMPLE_ALIAS;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.SIMPLE_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.TABLE_NAME;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.SIMPLE_SOURCE;
import static sjtu.ipads.wtune.sqlparser.plan.internal.NativePlanAttribute.fromColumn;

public class InputNodeImpl extends PlanNodeBase implements InputNode {
  private final Table table;
  private final String alias;
  private final List<PlanAttribute> attributes;

  private InputNodeImpl(Relation rel) {
    this.table = rel.table();
    this.alias = rel.alias();
    this.attributes = listMap(it -> fromColumn(alias, it), table.columns());
  }

  private InputNodeImpl(Table table, String alias, List<PlanAttribute> attributes) {
    this.table = table;
    this.alias = alias;
    this.attributes = attributes;
  }

  public static InputNode build(Relation rel) {
    return new InputNodeImpl(rel);
  }

  @Override
  public Table table() {
    return table;
  }

  @Override
  public List<PlanAttribute> usedAttributes() {
    return Collections.emptyList();
  }

  @Override
  public ASTNode toTableSource() {
    final ASTNode name = node(TABLE_NAME);
    name.set(TABLE_NAME_TABLE, table.name());

    final ASTNode tableSource = ASTNode.tableSource(SIMPLE_SOURCE);
    tableSource.set(SIMPLE_TABLE, name);
    tableSource.set(SIMPLE_ALIAS, alias);

    return tableSource;
  }

  @Override
  public List<PlanAttribute> outputAttributes() {
    return attributes;
  }

  @Override
  public void resolveUsedAttributes() {}

  @Override
  protected PlanNode copy0() {
    return new InputNodeImpl(table, alias, attributes);
  }
}
