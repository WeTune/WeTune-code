package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.common.utils.Pair;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.attrs.ColumnRef;
import sjtu.ipads.wtune.stmt.attrs.Param;
import sjtu.ipads.wtune.stmt.attrs.ParamModifier;
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
    node.accept(new ParamResolver());
    return true;
  }

  @Override
  public boolean enterChild(SQLNode parent, Attrs.Key<SQLNode> key, SQLNode child) {
    if (key == QUERY_LIMIT) {
      return false;

    } else if (key == QUERY_OFFSET) {
      if (child != null)
        child.put(
            RESOLVED_PARAM,
            new Param(child, Collections.singletonList(ParamModifier.of(GEN_OFFSET))));
      return false;
    }

    return true;
  }

  private static void resolveParam(SQLNode startPoint) {
    final Pair<SQLNode, SQLNode> pair = boolContext(startPoint);
    if (pair == null) return;
    final SQLNode ctx = pair.left();

    final LinkedList<ParamModifier> modifierStack = new LinkedList<>();
    SQLNode p = startPoint;

    do {
      if (!resolveReversedModifier(p, modifierStack)) return;
      p = p.parent();
    } while (p != ctx);

    if (modifierStack.getLast().type() == GUESS) {
      modifierStack.removeLast();
      if (exprKind(startPoint) == Kind.LITERAL)
        modifierStack.add(ParamModifier.of(DIRECT_VALUE, startPoint.get(LITERAL_VALUE)));
      else modifierStack.add(ParamModifier.of(DIRECT_VALUE, "UNKNOWN"));
    }

    startPoint.put(RESOLVED_PARAM, new Param(startPoint, modifierStack));
  }

  private static Pair<SQLNode, SQLNode> boolContext(SQLNode startPoint) {
    SQLNode child = startPoint, parent = startPoint.parent();

    while (isExpr(parent) && parent.get(BOOL_EXPR) == null) {
      child = parent;
      parent = parent.parent();
    }

    return parent.get(BOOL_EXPR) == null ? null : Pair.of(parent, child);
  }

  private static final Set<Class<? extends Resolver>> DEPENDENCIES =
      Set.of(BoolExprResolver.class, ColumnResolver.class);

  private static boolean resolveReversedModifier(SQLNode target, List<ParamModifier> stack) {
    final SQLNode parent = target.parent();
    final Kind kind = exprKind(parent);
    if (kind == Kind.UNARY) {
      final UnaryOp op = parent.get(UNARY_OP);
      if (op == UnaryOp.UNARY_MINUS) stack.add(ParamModifier.of(ParamModifier.Type.INVERSE));
      // omit others since not encountered
      else return op == UnaryOp.BINARY;

      return true;

    } else if (kind == Kind.BINARY) {
      final SQLNode otherSide = binaryOtherSide(parent, target);
      if (!resolveModifier(otherSide, stack)) return false;

      final BinaryOp op = parent.get(BINARY_OP);
      if (op == BinaryOp.EQUAL || op == BinaryOp.IN_LIST || op == BinaryOp.ARRAY_CONTAINS)
        return true;
      if (op == BinaryOp.NOT_EQUAL) return stack.add(ParamModifier.of(NEQ));
      else if (op == BinaryOp.GREATER_OR_EQUAL || op == BinaryOp.GREATER_THAN)
        stack.add(ParamModifier.of(DECREASE));
      else if (op == BinaryOp.LESS_OR_EQUAL || op == BinaryOp.LESS_THAN)
        stack.add(ParamModifier.of(INCREASE));
      else if (op == BinaryOp.LIKE || op == BinaryOp.ILIKE || op == BinaryOp.SIMILAR_TO)
        stack.add(ofLike(target));
      else if (op == BinaryOp.IS) stack.add(ofIs(target));
      else if (op.standard() == BinaryOp.REGEXP) stack.add(ParamModifier.of(REGEX));
      else if (op == BinaryOp.PLUS) stack.add(ParamModifier.of(SUBTRACT));
      else if (op == BinaryOp.MINUS) stack.add(ParamModifier.of(ADD));
      else if (op == BinaryOp.MULT) stack.add(ParamModifier.of(DIVIDE));
      else if (op == BinaryOp.DIV) stack.add(ParamModifier.of(TIMES));
      // omit others since not encountered
      else return false;

      return true;

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
      return true;

    } else if (kind == Kind.MATCH) {
      if (!resolveModifier(parent.get(MATCH_COLS).get(0), stack)) return false;
      stack.add(ParamModifier.of(MATCHING));
      return true;

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
        if (position == null) {
          System.out.println();
        }
        assert position != null;
        stack.add(
            ParamModifier.of(
                COLUMN_VALUE, column.table().tableName(), column.columnName(), position));
      }

    } else if (kind == Kind.FUNC_CALL) {
      for (SQLNode arg : target.get(FUNC_CALL_ARGS)) if (!resolveModifier(arg, stack)) return false;
      stack.add(ParamModifier.of(INVOKE_FUNC, target.get(FUNC_CALL_NAME).toString().toLowerCase()));

    } else if (kind == Kind.BINARY) {
      if (!resolveModifier(target.get(BINARY_LEFT), stack)
          || !resolveModifier(target.get(BINARY_RIGHT), stack)) return false;

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

    } else if (kind == Kind.AGGREGATE) {
      stack.add(ParamModifier.of(INVOKE_AGG, target.get(AGGREGATE_NAME)));

    } else if (kind == Kind.CAST) {
      resolveModifier(target.get(CAST_EXPR), stack);

    } else if (kind == Kind.LITERAL) {
      stack.add(ParamModifier.of(DIRECT_VALUE, target.get(LITERAL_VALUE)));

    } else if (kind == Kind.TUPLE) {
      for (SQLNode elements : target.get(TUPLE_EXPRS))
        if (!resolveModifier(elements, stack)) return false;
      stack.add(ParamModifier.of(MAKE_TUPLE));

    } else if (kind == Kind.SYMBOL) {
      stack.add(ParamModifier.of(DIRECT_VALUE, target.get(SYMBOL_TEXT)));

    } else if (kind == Kind.QUERY_EXPR) {
      stack.add(ParamModifier.of(GUESS));
    } else return false;

    return true;
  }

  private static ParamModifier ofLike(SQLNode param) {
    if (exprKind(param) == Kind.LITERAL) {
      final String value = param.get(LITERAL_VALUE).toString();
      return ParamModifier.of(LIKE, value.startsWith("%"), value.endsWith("%"));
    }
    return ParamModifier.of(LIKE);
  }

  private static ParamModifier ofIs(SQLNode param) {
    if (exprKind(param) == Kind.LITERAL) {
      if (param.get(LITERAL_TYPE) == LiteralType.NULL) return ParamModifier.of(CHECK_NULL);
      else if (param.get(LITERAL_TYPE) == LiteralType.BOOL) return ParamModifier.of(CHECK_BOOL);
      else return assertFalse();

    } else return assertFalse();
  }

  @Override
  public Set<Class<? extends Resolver>> dependsOn() {
    return DEPENDENCIES;
  }
}
