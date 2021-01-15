package sjtu.ipads.wtune.stmt.resolver;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.SQLVisitor;
import sjtu.ipads.wtune.sqlparser.ast.constants.*;
import sjtu.ipads.wtune.stmt.attrs.ColumnRef;
import sjtu.ipads.wtune.stmt.attrs.Param;
import sjtu.ipads.wtune.stmt.attrs.ParamModifier;
import sjtu.ipads.wtune.stmt.schema.Column;
import sjtu.ipads.wtune.stmt.Statement;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.assertFalse;
import static sjtu.ipads.wtune.sqlparser.ast.ExprAttrs.*;
import static sjtu.ipads.wtune.sqlparser.ast.NodeAttrs.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.EXPR;
import static sjtu.ipads.wtune.stmt.analyzer.Analysis.buildRelationGraph;
import static sjtu.ipads.wtune.stmt.attrs.ParamModifier.Type.*;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.*;

/**
 * Mark parameter in a statement.
 *
 * <p>SQLNode of following types may be marked as parameters:
 *
 * <ul>
 *   <li>Literal
 *   <li>ParamMarker
 * </ul>
 */
class ResolveParamFull implements SQLVisitor {

  public static void resolve(Statement stmt) {
    buildRelationGraph(stmt.parsed()).expanded().calcRelationPosition();
    stmt.parsed().accept(new ResolveParamFull());
  }

  @Override
  public boolean enterParamMarker(SQLNode paramMarker) {
    resolveParam(paramMarker);
    return false;
  }

  @Override
  public boolean enterLiteral(SQLNode literal) {
    resolveParam(literal);
    return false;
  }

  @Override
  public boolean enterChild(SQLNode parent, Attrs.Key<SQLNode> key, SQLNode child) {
    if (key == QUERY_LIMIT) {
      if (child != null && PARAM_MARKER.isInstance(child)) {
        child.remove(PARAM_MARKER_NUMBER);
        child.put(EXPR_KIND, LITERAL);
        child.put(LITERAL_TYPE, LiteralType.INTEGER);
        child.put(LITERAL_VALUE, 10);
      }
      return false;

    } else if (key == QUERY_OFFSET) {
      if (child != null)
        child.put(
            RESOLVED_PARAM,
            new Param(
                child.get(PARAM_INDEX),
                child,
                Collections.singletonList(ParamModifier.of(GEN_OFFSET))));
      return false;
    }

    return true;
  }

  private static void resolveParam(SQLNode startPoint) {
    final Pair<SQLNode, SQLNode> pair = boolContext(startPoint);
    if (pair == null) return;
    final SQLNode ctx = pair.getLeft();

    boolean not = false;
    SQLNode parent = ctx.parent();
    while (EXPR.isInstance(parent)) {
      if (parent.get(UNARY_OP) == UnaryOp.NOT) not = !not;
      parent = parent.parent();
    }

    final LinkedList<ParamModifier> modifierStack = new LinkedList<>();
    SQLNode p = startPoint;

    do {
      if (!resolveReversedModifier(p, modifierStack, not)) return;
      p = p.parent();
    } while (p != ctx);

    if (modifierStack.getLast().type() == GUESS) {
      modifierStack.removeLast();
      if (LITERAL.isInstance(startPoint))
        modifierStack.add(ParamModifier.of(DIRECT_VALUE, startPoint.get(LITERAL_VALUE)));
      else modifierStack.add(ParamModifier.of(DIRECT_VALUE, "UNKNOWN"));
    }

    startPoint.put(
        RESOLVED_PARAM, new Param(startPoint.get(PARAM_INDEX), startPoint, modifierStack));
  }

  private static Pair<SQLNode, SQLNode> boolContext(SQLNode startPoint) {
    SQLNode child = startPoint, parent = startPoint.parent();

    while (EXPR.isInstance(parent) && parent.get(BOOL_EXPR) == null) {
      child = parent;
      parent = parent.parent();
    }

    return parent.get(BOOL_EXPR) == null ? null : Pair.of(parent, child);
  }

  private static boolean resolveReversedModifier(
      SQLNode target, List<ParamModifier> stack, boolean not) {
    final SQLNode parent = target.parent();
    final ExprType exprKind = parent.get(EXPR_KIND);

    if (exprKind == UNARY) {
      final UnaryOp op = parent.get(UNARY_OP);
      if (op == UnaryOp.UNARY_MINUS) stack.add(ParamModifier.of(ParamModifier.Type.INVERSE));
      // omit others since not encountered
      else return op == UnaryOp.BINARY;

      return true;

    } else if (exprKind == BINARY) {
      // if target is on the right, the operator should be reversed
      final SQLNode left = parent.get(BINARY_LEFT);
      final SQLNode right = parent.get(BINARY_RIGHT);
      final boolean inverseOp = right == target;
      final SQLNode otherSide = inverseOp ? left : right;
      final BinaryOp op = parent.get(BINARY_OP);

      final ParamModifier modifier = ParamModifier.fromBinaryOp(op, target, inverseOp, not);
      if (modifier == null) return false;

      if (modifier.type() != KEEP) stack.add(modifier);

      return resolveModifier(otherSide, stack);

    } else if (exprKind == TUPLE) {
      stack.add(ParamModifier.of(TUPLE_ELEMENT));
      return true;

    } else if (exprKind == ARRAY) {
      stack.add(ParamModifier.of(ARRAY_ELEMENT));
      return true;

    } else if (exprKind == TERNARY) {
      if (parent.get(TERNARY_MIDDLE) == target) stack.add(ParamModifier.of(DECREASE));
      else if (parent.get(TERNARY_RIGHT) == target) stack.add(ParamModifier.of(INCREASE));
      else return assertFalse();

      return resolveModifier(parent.get(TERNARY_LEFT), stack);

    } else if (exprKind == MATCH) {
      stack.add(ParamModifier.of(MATCHING));
      return resolveModifier(parent.get(MATCH_COLS).get(0), stack);

    } else return false;
  }

  private static boolean resolveModifier(SQLNode target, List<ParamModifier> stack) {
    final ExprType exprKind = target.get(EXPR_KIND);

    if (exprKind == COLUMN_REF) {
      final ColumnRef cRef = target.get(RESOLVED_COLUMN_REF);
      final Column column = cRef.resolveAsColumn();
      if (column == null) {
        stack.add(ParamModifier.of(GUESS));

      } else {
        final Integer position = cRef.resolveRootRef().source().node().get(RELATION_POSITION);
        assert position != null;
        stack.add(
            ParamModifier.of(
                COLUMN_VALUE, column.table().tableName(), column.columnName(), position));
      }

    } else if (exprKind == FUNC_CALL) {
      final List<SQLNode> args = target.get(FUNC_CALL_ARGS);
      stack.add(
          ParamModifier.of(
              INVOKE_FUNC, target.get(FUNC_CALL_NAME).toString().toLowerCase(), args.size()));
      for (SQLNode arg : Lists.reverse(args)) if (!resolveModifier(arg, stack)) return false;

    } else if (exprKind == BINARY) {
      switch (target.get(BINARY_OP)) {
        case PLUS:
          stack.add(ParamModifier.of(ADD));
          break;
        case MINUS:
          stack.add(ParamModifier.of(SUBTRACT));
          break;
        case MULT:
          stack.add(ParamModifier.of(TIMES));
          break;
        case DIV:
          stack.add(ParamModifier.of(DIVIDE));
          break;
        default:
          return false;
      }

      return resolveModifier(target.get(BINARY_RIGHT), stack)
          && resolveModifier(target.get(BINARY_LEFT), stack);

    } else if (exprKind == AGGREGATE) {
      stack.add(ParamModifier.of(INVOKE_AGG, target.get(AGGREGATE_NAME)));

    } else if (exprKind == CAST) {
      if (target.get(CAST_TYPE).category() == Category.INTERVAL) return false;

      resolveModifier(target.get(CAST_EXPR), stack);

    } else if (exprKind == LITERAL) {
      stack.add(ParamModifier.of(DIRECT_VALUE, target.get(LITERAL_VALUE)));

    } else if (exprKind == TUPLE) {
      final List<SQLNode> exprs = target.get(TUPLE_EXPRS);
      stack.add(ParamModifier.of(MAKE_TUPLE, exprs.size()));
      for (SQLNode elements : Lists.reverse(exprs))
        if (!resolveModifier(elements, stack)) return false;

    } else if (exprKind == SYMBOL) {
      stack.add(ParamModifier.of(DIRECT_VALUE, target.get(SYMBOL_TEXT)));

    } else if (exprKind == QUERY_EXPR) {
      stack.add(ParamModifier.of(GUESS));
    } else return false;

    return true;
  }
}
