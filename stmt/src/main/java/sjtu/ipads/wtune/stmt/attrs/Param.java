package sjtu.ipads.wtune.stmt.attrs;

import sjtu.ipads.wtune.sqlparser.SQLNode;

import java.util.List;

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
}
