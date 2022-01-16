package sjtu.ipads.wtune.sql.util;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import sjtu.ipads.wtune.common.utils.Lazy;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.sql.ast.SqlNodes;
import sjtu.ipads.wtune.sql.ast.constants.LiteralKind;
import sjtu.ipads.wtune.sql.schema.Column;
import sjtu.ipads.wtune.sql.support.locator.LocatorSupport;
import sjtu.ipads.wtune.sql.support.resolution.ParamDesc;
import sjtu.ipads.wtune.sql.support.resolution.ParamModifier;
import sjtu.ipads.wtune.sql.support.resolution.Params;
import sjtu.ipads.wtune.sql.support.resolution.ResolutionSupport;

import java.util.ArrayList;

import static sjtu.ipads.wtune.common.utils.ListSupport.elemAt;
import static sjtu.ipads.wtune.common.utils.ListSupport.tail;
import static sjtu.ipads.wtune.sql.ast.ExprFields.*;
import static sjtu.ipads.wtune.sql.ast.ExprKind.Literal;
import static sjtu.ipads.wtune.sql.ast.ExprKind.Param;
import static sjtu.ipads.wtune.sql.ast.SqlNodeFields.Expr_Kind;
import static sjtu.ipads.wtune.sql.support.resolution.ParamModifier.Type.*;
import static sjtu.ipads.wtune.sql.support.resolution.Params.PARAMS;

public class ParamInterpolator {
  private final SqlNode ast;
  private final Lazy<TIntList> interpolations;

  public ParamInterpolator(SqlNode ast) {
    this.ast = ast;
    this.interpolations = Lazy.mk(TIntArrayList::new);
  }

  public void go() {
    ResolutionSupport.setLimitClauseAsParam(false);
    final Params params = ast.context().getAdditionalInfo(PARAMS);
    final SqlNodes paramNodes = LocatorSupport.nodeLocator().accept(Param).gather(ast);
    for (SqlNode paramNode : paramNodes) interpolateOne(params.paramOf(paramNode));
  }

  public void undo() {
    if (interpolations.isInitialized()) {
      final TIntList nodeIds = interpolations.get();
      for (int i = 0, bound = nodeIds.size(); i < bound; ++i) {
        final SqlNode node = SqlNode.mk(ast.context(), nodeIds.get(i));
        node.$(Expr_Kind, Param);
        node.$(Param_Number, i + 1);
        node.remove(Literal_Kind);
        node.remove(Literal_Value);
      }
    }
  }

  private void interpolateOne(ParamDesc param) {
    final SqlNode paramNode = param.node();
    if (!Param.isInstance(paramNode)) return;

    ParamModifier modifier = tail(param.modifiers());
    if (modifier == null) return;
    if (modifier.type() == TUPLE_ELEMENT || modifier.type() == ARRAY_ELEMENT)
      modifier = elemAt(param.modifiers(), -2);
    if (modifier == null || modifier.type() != COLUMN_VALUE) return;

    final SqlNode valueNode = mkValue(((Column) modifier.args()[1]));
    ast.context().displaceNode(paramNode.nodeId(), valueNode.nodeId());

    interpolations.get().add(paramNode.nodeId());
  }

  private SqlNode mkValue(Column column) {
    final SqlNode value = SqlNode.mk(ast.context(), Literal);
    switch (column.dataType().category()) {
      case INTEGRAL:
        value.$(Literal_Kind, LiteralKind.INTEGER);
        value.$(Literal_Value, 1);
        break;
      case FRACTION:
        value.$(Literal_Kind, LiteralKind.FRACTIONAL);
        value.$(Literal_Value, 1.0);
        break;
      case BOOLEAN:
        value.$(Literal_Kind, LiteralKind.BOOL);
        value.$(Literal_Value, false);
        break;
      case STRING:
        value.$(Literal_Kind, LiteralKind.TEXT);
        value.$(Literal_Value, "00001");
        break;
      case TIME:
        value.$(Literal_Kind, LiteralKind.TEXT);
        value.$(Literal_Value, "2021-01-01 00:00:00.000");
        break;
      default:
        value.$(Literal_Kind, LiteralKind.NULL);
        break;
    }
    return value;
  }
}
