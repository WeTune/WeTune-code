package sjtu.ipads.wtune.stmt.mutator;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.Collections;
import java.util.List;

import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.ATTR_PREFIX;

/**
 * Clean stmt.
 *
 * <p>1. Remove constant expression like "1=1"
 */
public class Cleaner implements Mutator {
  private static final Attrs.Key<Boolean> IS_CONSTANT =
      Attrs.Key.of(ATTR_PREFIX + "cleaner.isConstant", Boolean.class);

  @Override
  public void mutate(Statement stmt) {
    final SQLNode node = stmt.parsed();
    node.accept(MARK);
    node.accept(SWIPE);
    node.accept(TEXT);
    node.relinkAll();
  }

  private static final SQLVisitor MARK = new Mark();
  private static final SQLVisitor SWIPE = new Swipe();
  private static final SQLVisitor TEXT = new TextNormalizer();

  private static class Mark implements SQLVisitor {
    @Override
    public void leave(SQLNode node) {
      if (isConstant(node)) node.flag(IS_CONSTANT);
    }
  }

  private static class Swipe implements SQLVisitor {
    @Override
    public boolean isMutator() {
      return true;
    }

    @Override
    public boolean enter(SQLNode node) {
      if (!node.isFlagged(IS_CONSTANT)) return true;

      final SQLNode parent = node.parent();
      if (parent == null) {
        node.invalidate();
        node.flagStructChanged(true);
        return false;
      }

      if (parent.type() == SQLNode.Type.QUERY_SPEC && parent.get(QUERY_SPEC_WHERE) == node) {
        parent.put(QUERY_SPEC_WHERE, null);
        return false;
      }

      final Kind parentKind = exprKind(parent);
      if (parentKind == Kind.BINARY) {
        final SQLNode left = parent.get(BINARY_LEFT);
        final SQLNode right = parent.get(BINARY_RIGHT);
        final BinaryOp op = parent.get(BINARY_OP);
        if (op.isLogic()) {
          if (left == node) parent.replaceThis(right);
          else if (right == node) parent.replaceThis(left);
          else assert false;

          parent.flagStructChanged(true);
        }
      }

      return false;
    }
  }

  private static class TextNormalizer implements SQLVisitor {
    @Override
    public void leaveFuncCall(SQLNode funcCall) {
      final SQLNode name = funcCall.get(FUNC_CALL_NAME);
      if (name.get(NAME_2_0) != null) return;
      if ("concat".equalsIgnoreCase(name.get(NAME_2_1))) {
        final List<SQLNode> args = funcCall.get(FUNC_CALL_ARGS);
        final StringBuilder builder = new StringBuilder();
        for (SQLNode arg : args) {
          if (exprKind(arg) != Kind.LITERAL) return;
          builder.append(arg.get(LITERAL_VALUE).toString());
        }

        funcCall.put(EXPR_KIND, Kind.LITERAL);
        funcCall.put(LITERAL_TYPE, LiteralType.TEXT);
        funcCall.put(LITERAL_VALUE, builder.toString());
        funcCall.remove(FUNC_CALL_ARGS);
        funcCall.remove(FUNC_CALL_NAME);
        return;
      }

      if ("lower".equalsIgnoreCase(name.get(NAME_2_1))) {
        final List<SQLNode> args = funcCall.get(FUNC_CALL_ARGS);
        if (args.size() == 1 && exprKind(args.get(0)) == Kind.LITERAL) {
          funcCall.put(EXPR_KIND, Kind.LITERAL);
          funcCall.put(LITERAL_TYPE, LiteralType.TEXT);
          funcCall.put(LITERAL_VALUE, args.get(0).get(LITERAL_VALUE).toString().toLowerCase());
          funcCall.remove(FUNC_CALL_ARGS);
          funcCall.remove(FUNC_CALL_NAME);
        }
        return;
      }

      if ("upper".equalsIgnoreCase(name.get(NAME_2_1))) {
        final List<SQLNode> args = funcCall.get(FUNC_CALL_ARGS);
        if (args.size() == 1 && exprKind(args.get(0)) == Kind.LITERAL) {
          funcCall.put(EXPR_KIND, Kind.LITERAL);
          funcCall.put(LITERAL_VALUE, args.get(0).get(LITERAL_VALUE).toString().toUpperCase());
          funcCall.remove(FUNC_CALL_ARGS);
          funcCall.remove(FUNC_CALL_NAME);
        }
      }
    }
  }

  private static boolean isConstant(SQLNode node) {
    if (node == null || !isExpr(node)) return false;
    if (node.isFlagged(IS_CONSTANT)) return true;
    final Kind kind = exprKind(node);
    if (kind == Kind.LITERAL || kind == Kind.SYMBOL) return true;

    if (kind == Kind.CAST) return isConstant(node.get(CAST_EXPR));
    if (kind == Kind.COLLATE) return isConstant(node.get(COLLATE_EXPR));
    if (kind == Kind.INTERVAL) return isConstant(node.get(INTERVAL_EXPR));
    if (kind == Kind.CONVERT_USING) return isConstant(node.get(CONVERT_USING_EXPR));
    if (kind == Kind.DEFAULT) return isConstant(node.get(DEFAULT_COL));
    if (kind == Kind.VALUES) return isConstant(node.get(VALUES_EXPR));

    if (kind == Kind.UNARY) return isConstant(node.get(UNARY_EXPR));
    if (kind == Kind.BINARY)
      return isConstant(node.get(BINARY_LEFT)) && isConstant(node.get(BINARY_RIGHT));
    if (kind == Kind.TERNARY)
      return isConstant(node.get(TERNARY_LEFT))
          && isConstant(node.get(TERNARY_MIDDLE))
          && isConstant(node.get(TERNARY_RIGHT));

    if (kind == Kind.TUPLE) return node.get(TUPLE_EXPRS).stream().allMatch(Cleaner::isConstant);
    if (kind == Kind.FUNC_CALL)
      return !node.get(FUNC_CALL_NAME).get(NAME_2_1).contains("rand")
          && node.get(FUNC_CALL_ARGS).stream().allMatch(Cleaner::isConstant);

    if (kind == Kind.MATCH)
      return isConstant(node.get(MATCH_EXPR))
          && node.get(MATCH_COLS).stream().allMatch(Cleaner::isConstant);

    if (kind == Kind.CASE) {
      final SQLNode cond = node.get(CASE_COND);
      final SQLNode _else = node.get(CASE_ELSE);
      return (cond == null || isConstant(cond))
          && node.get(CASE_WHENS).stream().allMatch(Cleaner::isConstant)
          && (_else == null || isConstant(_else));
    }
    if (kind == Kind.WHEN)
      return isConstant(node.get(WHEN_COND)) && isConstant(node.get(WHEN_EXPR));

    return false;
  }
}
