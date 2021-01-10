package sjtu.ipads.wtune.stmt.attrs;


import sjtu.ipads.wtune.sqlparser.ast.SQLNode;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class Param {
  private final int index;
  private final List<ParamModifier> modifiers;
  private final SQLNode paramNode;

  public Param(int index, SQLNode paramNode, List<ParamModifier> modifiers) {
    this.index = index;
    this.paramNode = paramNode;
    this.modifiers = modifiers;
  }

  public int index() {
    return index;
  }

  public List<ParamModifier> modifiers() {
    return modifiers;
  }

  @Override
  public String toString() {
    return String.join(", ", listMap(ParamModifier::toString, modifiers));
  }
}
