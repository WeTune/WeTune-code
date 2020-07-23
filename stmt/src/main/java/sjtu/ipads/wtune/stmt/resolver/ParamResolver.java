package sjtu.ipads.wtune.stmt.resolver;

import com.google.common.collect.Lists;
import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.common.utils.Pair;
import sjtu.ipads.wtune.sqlparser.SQLDataType;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.attrs.ColumnRef;
import sjtu.ipads.wtune.stmt.attrs.Param;
import sjtu.ipads.wtune.stmt.attrs.ParamModifier;
import sjtu.ipads.wtune.stmt.mutator.TupleElementsNormalizer;
import sjtu.ipads.wtune.stmt.schema.Column;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.common.utils.Commons.assertFalse;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_LIMIT;
import static sjtu.ipads.wtune.sqlparser.SQLNode.QUERY_OFFSET;
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
public class ParamResolver implements SQLVisitor, Resolver {
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
  public boolean resolve(Statement stmt, SQLNode node) {
    stmt.relationGraph().expanded().calcRelationPosition();
    stmt.mutate(TupleElementsNormalizer.class);
    stmt.resolve(SimpleParamResolver.class, true);
    stmt.parsed().accept(new ParamResolver());
    return true;
  }

  @Override
  public boolean enterChild(SQLNode parent, Attrs.Key<SQLNode> key, SQLNode child) {
    if (key == QUERY_LIMIT) {
      if (child != null && exprKind(child) == Kind.PARAM_MARKER) {
        child.remove(PARAM_MARKER_NUMBER);
        child.put(EXPR_KIND, Kind.LITERAL);
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
    final SQLNode ctx = pair.left();

    boolean not = false;
    SQLNode parent = ctx.parent();
    while (isExpr(parent)) {
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
      if (exprKind(startPoint) == Kind.LITERAL)
        modifierStack.add(ParamModifier.of(DIRECT_VALUE, startPoint.get(LITERAL_VALUE)));
      else modifierStack.add(ParamModifier.of(DIRECT_VALUE, "UNKNOWN"));
    }

    startPoint.put(
        RESOLVED_PARAM, new Param(startPoint.get(PARAM_INDEX), startPoint, modifierStack));
  }

  private static Pair<SQLNode, SQLNode> boolContext(SQLNode startPoint) {
    SQLNode child = startPoint, parent = startPoint.parent();

    while (isExpr(parent) && parent.get(BOOL_EXPR) == null) {
      child = parent;
      parent = parent.parent();
    }

    return parent.get(BOOL_EXPR) == null ? null : Pair.of(parent, child);
  }

  private static boolean resolveReversedModifier(
      SQLNode target, List<ParamModifier> stack, boolean not) {
    final SQLNode parent = target.parent();
    final Kind kind = exprKind(parent);

    if (kind == Kind.UNARY) {
      final UnaryOp op = parent.get(UNARY_OP);
      if (op == UnaryOp.UNARY_MINUS) stack.add(ParamModifier.of(ParamModifier.Type.INVERSE));
      // omit others since not encountered
      else return op == UnaryOp.BINARY;

      return true;

    } else if (kind == Kind.BINARY) {
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

    } else if (kind == Kind.TUPLE) {
      stack.add(ParamModifier.of(TUPLE_ELEMENT));
      return true;

    } else if (kind == Kind.ARRAY) {
      stack.add(ParamModifier.of(ARRAY_ELEMENT));
      return true;

    } else if (kind == Kind.TERNARY) {
      if (parent.get(TERNARY_MIDDLE) == target) stack.add(ParamModifier.of(DECREASE));
      else if (parent.get(TERNARY_RIGHT) == target) stack.add(ParamModifier.of(INCREASE));
      else return assertFalse();

      return resolveModifier(parent.get(TERNARY_LEFT), stack);

    } else if (kind == Kind.MATCH) {
      stack.add(ParamModifier.of(MATCHING));
      return resolveModifier(parent.get(MATCH_COLS).get(0), stack);

    } else return false;
  }

  private static boolean resolveModifier(SQLNode target, List<ParamModifier> stack) {
    final Kind kind = exprKind(target);

    if (kind == Kind.COLUMN_REF) {
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

    } else if (kind == Kind.FUNC_CALL) {
      final List<SQLNode> args = target.get(FUNC_CALL_ARGS);
      stack.add(
          ParamModifier.of(
              INVOKE_FUNC, target.get(FUNC_CALL_NAME).toString().toLowerCase(), args.size()));
      for (SQLNode arg : Lists.reverse(args)) if (!resolveModifier(arg, stack)) return false;

    } else if (kind == Kind.BINARY) {
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

    } else if (kind == Kind.AGGREGATE) {
      stack.add(ParamModifier.of(INVOKE_AGG, target.get(AGGREGATE_NAME)));

    } else if (kind == Kind.CAST) {
      if (target.get(CAST_TYPE).category() == SQLDataType.Category.INTERVAL) return false;

      resolveModifier(target.get(CAST_EXPR), stack);

    } else if (kind == Kind.LITERAL) {
      stack.add(ParamModifier.of(DIRECT_VALUE, target.get(LITERAL_VALUE)));

    } else if (kind == Kind.TUPLE) {
      final List<SQLNode> exprs = target.get(TUPLE_EXPRS);
      stack.add(ParamModifier.of(MAKE_TUPLE, exprs.size()));
      for (SQLNode elements : Lists.reverse(exprs))
        if (!resolveModifier(elements, stack)) return false;

    } else if (kind == Kind.SYMBOL) {
      stack.add(ParamModifier.of(DIRECT_VALUE, target.get(SYMBOL_TEXT)));

    } else if (kind == Kind.QUERY_EXPR) {
      stack.add(ParamModifier.of(GUESS));
    } else return false;

    return true;
  }

  private static final Set<Class<? extends Resolver>> DEPENDENCIES =
      Set.of(BoolExprResolver.class, ColumnResolver.class);

  @Override
  public Set<Class<? extends Resolver>> dependsOn() {
    return DEPENDENCIES;
  }
}
