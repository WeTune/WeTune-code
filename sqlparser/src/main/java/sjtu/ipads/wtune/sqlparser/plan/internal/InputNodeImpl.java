package sjtu.ipads.wtune.sqlparser.plan.internal;

import static sjtu.ipads.wtune.common.utils.Commons.coalesce;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.TABLE_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.SIMPLE_ALIAS;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.SIMPLE_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.TABLE_NAME;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.SIMPLE_SOURCE;
import static sjtu.ipads.wtune.sqlparser.plan.AttributeDef.fromColumn;
import static sjtu.ipads.wtune.sqlparser.plan.AttributeDefBag.makeBag;

import java.util.Collections;
import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDefBag;
import sjtu.ipads.wtune.sqlparser.plan.InputNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Table;

public class InputNodeImpl extends PlanNodeBase implements InputNode {
  private final int id;
  private final Table table;
  private final ASTNode node;
  private AttributeDefBag definedAttrs;
  private String alias;

  // for `copy`
  private InputNodeImpl(ASTNode node, int id, Table table, String alias, AttributeDefBag attrs) {
    this.node = node;
    this.id = id;
    this.table = table;
    this.alias = alias;
    this.definedAttrs = attrs;
  }

  public static InputNode build(ASTNode node, Table table, String alias) {
    final String nonNullAlias = coalesce(alias, table.name());
    final int id = System.identityHashCode(new Object());
    final AttributeDefBag bag =
        makeBag(listMap(table.columns(), it -> makeAttribute(id, nonNullAlias, it)));
    return new InputNodeImpl(node, id, table, nonNullAlias, bag);
  }

  protected static AttributeDef makeAttribute(int key, String qualification, Column column) {
    return fromColumn(key * 31 + column.hashCode(), qualification, column);
  }

  @Override
  public Table table() {
    return this.table;
  }

  @Override
  public ASTNode node() {
    return node;
  }

  @Override
  public ASTNode tableSource() {
    final ASTNode name = ASTNode.node(TABLE_NAME);
    name.set(TABLE_NAME_TABLE, table.name());

    final ASTNode tableSource = ASTNode.tableSource(SIMPLE_SOURCE);
    tableSource.set(SIMPLE_TABLE, name);
    tableSource.set(SIMPLE_ALIAS, alias);

    return tableSource;
  }

  @Override
  public void setAlias(String alias) {
    this.alias = alias;
    this.definedAttrs = definedAttrs.copyWithQualification(alias);
  }

  @Override
  public List<AttributeDef> usedAttributes() {
    return Collections.emptyList();
  }

  @Override
  public AttributeDefBag definedAttributes() {
    return definedAttrs;
  }

  @Override
  protected PlanNode copy0() {
    return new InputNodeImpl(node, id, table, alias, definedAttrs);
  }

  @Override
  public String toString() {
    return "Input<%s AS %s>@%d".formatted(table.name(), alias, id);
  }
}
