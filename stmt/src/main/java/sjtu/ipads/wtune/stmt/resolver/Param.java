package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.sqlparser.ast.SQLNode;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class Param {
  private final SQLNode node;
  private final int index;
  private final List<ParamModifier> modifiers;

  public Param(SQLNode node, int index) {
    this.index = index;
    this.node = node;
    this.modifiers = null;
  }

  public Param(SQLNode node, int index, List<ParamModifier> modifiers) {
    this.index = index;
    this.node = node;
    this.modifiers = modifiers;
  }

  public int index() {
    return index;
  }

  public SQLNode node() {
    return node;
  }

  public List<ParamModifier> modifiers() {
    return modifiers;
  }

  @Override
  public String toString() {
    return String.join(", ", listMap(ParamModifier::toString, modifiers));
  }
}
