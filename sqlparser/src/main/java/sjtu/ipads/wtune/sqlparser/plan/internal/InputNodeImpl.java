package sjtu.ipads.wtune.sqlparser.plan.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.InputNode;
import sjtu.ipads.wtune.sqlparser.plan.OutputAttribute;
import sjtu.ipads.wtune.sqlparser.relational.Relation;
import sjtu.ipads.wtune.sqlparser.schema.Table;

import java.util.Collections;
import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.ASTNode.node;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.TABLE_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.SIMPLE_ALIAS;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.SIMPLE_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.TABLE_NAME;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.SIMPLE_SOURCE;
import static sjtu.ipads.wtune.sqlparser.plan.OutputAttribute.fromInput;

public class InputNodeImpl extends PlanNodeBase implements InputNode {
  private final Table table;
  private final String alias;
  private final List<OutputAttribute> attributes;

  private InputNodeImpl(Relation table) {
    this.table = table.table();
    this.alias = table.alias();
    this.attributes = fromInput(this, table);
  }

  public static InputNode build(Relation table) {
    return new InputNodeImpl(table);
  }

  @Override
  public ASTNode tableSource() {
    final ASTNode name = node(TABLE_NAME);
    name.set(TABLE_NAME_TABLE, table.name());

    final ASTNode tableSource = ASTNode.tableSource(SIMPLE_SOURCE);
    tableSource.set(SIMPLE_TABLE, name);
    tableSource.set(SIMPLE_ALIAS, alias);

    return tableSource;
  }

  @Override
  public List<OutputAttribute> outputAttributes() {
    return attributes;
  }

  @Override
  public List<OutputAttribute> usedAttributes() {
    return Collections.emptyList();
  }
}
