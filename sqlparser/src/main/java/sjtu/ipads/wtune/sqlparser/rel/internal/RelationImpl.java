package sjtu.ipads.wtune.sqlparser.rel.internal;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.rel.Attribute;
import sjtu.ipads.wtune.sqlparser.rel.Relation;
import sjtu.ipads.wtune.sqlparser.rel.Schema;
import sjtu.ipads.wtune.sqlparser.rel.Table;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.NodeAttrs.*;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceAttrs.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.DERIVED_SOURCE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceType.SIMPLE_SOURCE;

public class RelationImpl implements Relation {
  private final SQLNode node;
  private final String alias;
  private final Relation parent;
  private final List<Relation> inputs;

  private List<Attribute> attributes;

  private RelationImpl(SQLNode node, Relation parent, int expectedNumInputs) {
    this.node = node;
    this.alias = tableSourceName(node);
    this.parent = parent;
    this.inputs = new ArrayList<>(expectedNumInputs);
  }

  public static Relation build(SQLNode node) {
    final SQLNode parentNode = node.parent();
    final Relation parent = parentNode == null ? null : parentNode.get(RELATION);
    if (QUERY.isInstance(node) && DERIVED_SOURCE.isInstance(parentNode)) return parent;

    final int expectedNumInputs = SIMPLE_SOURCE.isInstance(node) ? 0 : 4;
    final Relation rel = new RelationImpl(node, parent, expectedNumInputs);

    if (parent != null && (TABLE_SOURCE.isInstance(node) || SET_OP.isInstance(parentNode)))
      parent.addInput(rel);

    return rel;
  }

  @Override
  public SQLNode node() {
    return node;
  }

  @Override
  public String alias() {
    return alias;
  }

  @Override
  public Relation parent() {
    return parent;
  }

  @Override
  public List<Relation> inputs() {
    return inputs;
  }

  @Override
  public List<Attribute> attributes() {
    if (attributes != null) return attributes;

    final Schema schema = node.context().schema();
    if (SIMPLE_SOURCE.isInstance(node)) {
      final Table table = schema.table(node.get(SIMPLE_TABLE).get(TABLE_NAME_TABLE));
      attributes = listMap(Attribute::fromColumn, table.columns());

    } else if (DERIVED_SOURCE.isInstance(node)) {
      attributes = outputAttributesOf(node.get(DERIVED_SUBQUERY));

    } else if (QUERY.isInstance(node)) {
      attributes = outputAttributesOf(node);
    }

    return attributes;
  }

  private List<Attribute> outputAttributesOf(SQLNode node) {
    if (QUERY_SPEC.isInstance(node)) {
      return listMap(Attribute::fromProjection, node.get(QUERY_SPEC_SELECT_ITEMS));

    } else if (QUERY.isInstance(node)) {
      return outputAttributesOf(node.get(QUERY_BODY));

    } else if (SET_OP.isInstance(node)) {
      return outputAttributesOf(node.get(SET_OP_LEFT));

    } else throw new AssertionError();
  }

  @Override
  public void addInput(Relation relation) {
    inputs.add(relation);
  }
}
