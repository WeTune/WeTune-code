package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.sqlparser.SQLNode;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

public class Param {
  private final List<ParamModifier> modifiers;
  private final SQLNode paramNode;

  public Param(SQLNode paramNode, List<ParamModifier> modifiers) {
    this.paramNode = paramNode;
    this.modifiers = modifiers;
  }

  public List<ParamModifier> modifiers() {
    return modifiers;
  }

  @Override
  public String toString() {
    return String.join(", ", listMap(ParamModifier::toString, modifiers));
  }
}
