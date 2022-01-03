package sjtu.ipads.wtune.sql.resolution;

import sjtu.ipads.wtune.sql.ast1.SqlNode;

import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.tail;
import static sjtu.ipads.wtune.common.utils.IterableSupport.any;
import static sjtu.ipads.wtune.sql.resolution.ParamModifier.Type.*;

class ParamDescImpl implements ParamDesc {
  private int index;

  private final transient SqlNode node;
  private final transient List<ParamModifier> modifiers;
  private final String exprString;

  ParamDescImpl(SqlNode expr, SqlNode node, List<ParamModifier> modifiers) {
    this.node = node;
    this.modifiers = modifiers;
    this.exprString = expr == null ? "param" : expr.toString();
  }

  @Override
  public int index() {
    return index;
  }

  @Override
  public SqlNode node() {
    return node;
  }

  @Override
  public List<ParamModifier> modifiers() {
    return modifiers;
  }

  @Override
  public void setIndex(int index) {
    this.index = index;
  }

  @Override
  public String toString() {
    return "{%d,%s}".formatted(index, exprString);
  }
}
