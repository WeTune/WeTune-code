package sjtu.ipads.wtune.sqlparser.pg;

import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLTableSource;
import sjtu.ipads.wtune.sqlparser.pg.internal.PGParser;
import sjtu.ipads.wtune.sqlparser.pg.internal.PGParserBaseVisitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.assertFalse;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.ConstraintType.PRIMARY;
import static sjtu.ipads.wtune.sqlparser.SQLNode.ConstraintType.UNIQUE;
import static sjtu.ipads.wtune.sqlparser.SQLNode.columnName;
import static sjtu.ipads.wtune.sqlparser.SQLNode.Type.*;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.*;
import static sjtu.ipads.wtune.sqlparser.pg.PGASTHelper.tableName;
import static sjtu.ipads.wtune.sqlparser.pg.PGASTHelper.*;

public class PGASTBuilder extends PGParserBaseVisitor<SQLNode> {

  @Override
  public SQLNode visitCreate_table_statement(PGParser.Create_table_statementContext ctx) {
    final SQLNode node = new SQLNode(CREATE_TABLE);

    node.put(CREATE_TABLE_NAME, tableName(stringifyIdentifier(ctx.name)));

    final var defCtx = ctx.define_table();
    final List<SQLNode> colDefs = new ArrayList<>();
    final List<SQLNode> constrDefs = new ArrayList<>();

    for (var colOrConstrDef : defCtx.define_columns().table_column_def()) {
      if (colOrConstrDef.table_column_definition() != null) {
        final SQLNode colDef = colOrConstrDef.table_column_definition().accept(this);
        if (colDef != null) colDefs.add(colDef);

      } else if (colOrConstrDef.tabl_constraint != null) {
        final SQLNode constrDef = colOrConstrDef.tabl_constraint.accept(this);
        if (constrDef != null) constrDefs.add(constrDef);
      }
    }

    node.put(CREATE_TABLE_COLUMNS, colDefs);
    node.put(CREATE_TABLE_CONSTRAINTS, constrDefs);

    return node;
  }

  @Override
  public SQLNode visitTable_column_definition(PGParser.Table_column_definitionContext ctx) {
    final SQLNode node = new SQLNode(COLUMN_DEF);
    node.put(COLUMN_DEF_NAME, columnName(null, stringifyIdentifier(ctx.identifier())));

    final var dataTypeCtx = ctx.data_type();
    node.put(COLUMN_DEF_DATATYPE_RAW, dataTypeCtx.getText());
    node.put(COLUMN_DEF_DATATYPE, parseDataType(dataTypeCtx));

    for (var constr : ctx.constraint_common()) addConstraint(node, constr.constr_body());

    return node;
  }

  @Override
  public SQLNode visitConstraint_common(PGParser.Constraint_commonContext ctx) {
    final SQLNode node = new SQLNode(INDEX_DEF);
    if (ctx.identifier() != null) node.put(INDEX_DEF_NAME, stringifyIdentifier(ctx.identifier()));
    // only EXCLUDE and FOREIGN key can be defined at table level
    final var bodyCtx = ctx.constr_body();
    if (bodyCtx.REFERENCES() != null) {
      node.put(INDEX_DEF_CONS, ConstraintType.FOREIGN);
      final var namesLists = bodyCtx.names_in_parens();

      // should be 2, otherwise it's actually invalid
      if (namesLists.size() == 2)
        node.put(INDEX_DEF_KEYS, keyParts(namesLists.get(0).names_references()));

      final SQLNode ref = new SQLNode(REFERENCES);
      ref.put(REFERENCES_TABLE, tableName(stringifyIdentifier(bodyCtx.schema_qualified_name())));
      if (bodyCtx.ref != null)
        ref.put(REFERENCES_COLUMNS, columnNames(bodyCtx.ref.names_references()));

      node.put(INDEX_DEF_REFS, ref);

    } else if (bodyCtx.UNIQUE() != null || bodyCtx.PRIMARY() != null) {
      node.put(INDEX_DEF_CONS, bodyCtx.UNIQUE() != null ? UNIQUE : PRIMARY);
      node.put(INDEX_DEF_KEYS, keyParts(bodyCtx.names_in_parens(0).names_references()));

    } else return null;

    return node;
  }

  @Override
  public SQLNode visitAlter_sequence_statement(PGParser.Alter_sequence_statementContext ctx) {
    final SQLNode node = new SQLNode(ALTER_SEQUENCE);
    node.put(ALTER_SEQUENCE_NAME, commonName(stringifyIdentifier(ctx.name)));

    for (var bodyCtx : ctx.sequence_body()) {
      if (bodyCtx.OWNED() != null) {
        node.put(ALTER_SEQUENCE_OPERATION, "owned_by");
        node.put(
            ALTER_SEQUENCE_PAYLOAD, PGASTHelper.columnName(stringifyIdentifier(bodyCtx.col_name)));
      }
      // currently we only handle ALTER .. OWNED BY.
      // because it makes a column auto-increment, which concerns data gen
    }

    return node;
  }

  @Override
  public SQLNode visitAlter_table_statement(PGParser.Alter_table_statementContext ctx) {
    final SQLNode node = new SQLNode(ALTER_TABLE);
    node.put(ALTER_TABLE_NAME, tableName(stringifyIdentifier(ctx.name)));

    final List<SQLNode> actions = new ArrayList<>();

    final var actionCtxs = ctx.table_action();
    if (actionCtxs != null)
      for (var actionCtx : actionCtxs)
        if (actionCtx.tabl_constraint != null) {
          final SQLNode action = new SQLNode(ALTER_TABLE_ACTION);
          action.put(ALTER_TABLE_ACTION_NAME, "add_constraint");
          final SQLNode constraint = actionCtx.tabl_constraint.accept(this);
          if (constraint == null) actionCtx.tabl_constraint.accept(this);
          action.put(ALTER_TABLE_ACTION_PAYLOAD, constraint);
          actions.add(action);
        }

    // we only care about ADD CONSTRAINT fow now

    node.put(ALTER_TABLE_ACTIONS, actions);
    return node;
  }

  @Override
  public SQLNode visitCreate_index_statement(PGParser.Create_index_statementContext ctx) {
    final SQLNode node = new SQLNode(INDEX_DEF);
    node.put(INDEX_DEF_TABLE, tableName(stringifyIdentifier(ctx.table_name)));
    node.put(INDEX_DEF_NAME, stringifyIdentifier(ctx.name));
    if (ctx.UNIQUE() != null) node.put(INDEX_DEF_CONS, UNIQUE);

    final var restCtx = ctx.index_rest();
    if (restCtx.method != null) {
      final var methodText = restCtx.method.getText().toLowerCase();
      node.put(INDEX_DEF_TYPE, parseIndexType(methodText));
    }

    final List<SQLNode> keyParts =
        listMap(
            this::visitSort_specifier, restCtx.index_sort().sort_specifier_list().sort_specifier());

    node.put(INDEX_DEF_KEYS, keyParts);

    return node;
  }

  @Override
  public SQLNode visitSort_specifier(PGParser.Sort_specifierContext ctx) {
    final SQLNode node = new SQLNode(KEY_PART);
    final SQLNode expr = ctx.vex().accept(this);

    if (exprKind(expr) == SQLExpr.Kind.COLUMN_REF)
      node.put(KEY_PART_COLUMN, expr.get(COLUMN_REF_COLUMN).get(COLUMN_NAME_COLUMN));
    else node.put(KEY_PART_EXPR, expr);

    if (ctx.order != null) {
      final String direction = ctx.order.getText().toLowerCase();
      if ("asc".equals(direction)) node.put(KEY_PART_DIRECTION, KeyDirection.ASC);
      else if ("desc".equals(direction)) node.put(KEY_PART_DIRECTION, KeyDirection.DESC);
    }

    return node;
  }

  @Override
  public SQLNode visitSelect_stmt(PGParser.Select_stmtContext ctx) {
    if (ctx.with_clause() != null) return null;
    final SQLNode node = new SQLNode(QUERY);
    node.put(QUERY_BODY, ctx.select_ops().accept(this));
    ctx.after_ops().forEach(it -> addAfterOp(node, it));
    return node;
  }

  @Override
  public SQLNode visitSelect_ops(PGParser.Select_opsContext ctx) {
    if (ctx.select_stmt() != null) return ctx.select_stmt().accept(this);
    else if (ctx.select_primary() != null) return ctx.select_primary().accept(this);
    else {
      final SQLNode node = new SQLNode(SET_OPERATION);
      node.put(SET_OPERATION_LEFT, ctx.select_ops(0).accept(this));
      node.put(SET_OPERATION_RIGHT, ctx.select_ops(1).accept(this));
      final SetOperation op;
      if (ctx.UNION() != null) op = SetOperation.UNION;
      else if (ctx.INTERSECT() != null) op = SetOperation.INTERSECT;
      else if (ctx.EXCEPT() != null) op = SetOperation.EXCEPT;
      else return assertFalse();

      node.put(SET_OPERATION_TYPE, op);
      if (ctx.set_qualifier() != null)
        node.put(
            SET_OPERATION_OPTION,
            SetOperationOption.valueOf(ctx.set_qualifier().getText().toUpperCase()));
      return node;
    }
  }

  @Override
  public SQLNode visitSelect_ops_no_parens(PGParser.Select_ops_no_parensContext ctx) {
    if (ctx.select_ops() != null) {
      final SQLNode node = new SQLNode(SET_OPERATION);
      node.put(SET_OPERATION_LEFT, ctx.select_ops().accept(this));
      node.put(
          SET_OPERATION_RIGHT,
          ctx.select_primary() != null
              ? ctx.select_primary().accept(this)
              : ctx.select_stmt().accept(this));
      final SetOperation op;
      if (ctx.UNION() != null) op = SetOperation.UNION;
      else if (ctx.INTERSECT() != null) op = SetOperation.INTERSECT;
      else if (ctx.EXCEPT() != null) op = SetOperation.EXCEPT;
      else return assertFalse();

      node.put(SET_OPERATION_TYPE, op);
      if (ctx.set_qualifier() != null)
        node.put(
            SET_OPERATION_OPTION,
            SetOperationOption.valueOf(ctx.set_qualifier().getText().toUpperCase()));
      return node;

    } else if (ctx.select_primary() != null) return ctx.select_primary().accept(this);
    else return assertFalse();
  }

  @Override
  public SQLNode visitSelect_primary(PGParser.Select_primaryContext ctx) {
    final SQLNode node = new SQLNode(QUERY_SPEC);
    if (ctx.where != null) node.put(QUERY_SPEC_WHERE, ctx.where.accept(this));
    if (ctx.having != null) node.put(QUERY_SPEC_HAVING, ctx.having.accept(this));

    if (ctx.select_list() != null)
      node.put(
          QUERY_SPEC_SELECT_ITEMS,
          listMap(this::visitSelect_sublist, ctx.select_list().select_sublist()));

    if (ctx.groupby_clause() != null)
      node.put(
          QUERY_SPEC_GROUP_BY,
          listMap(
              this::visitGrouping_element,
              ctx.groupby_clause().grouping_element_list().grouping_element()));

    final var fromItems = ctx.from_item();
    if (fromItems != null) {
      final SQLNode tableSourceNode =
          fromItems.stream()
              .map(this::visitFrom_item)
              .reduce((left, right) -> joined(left, right, JoinType.CROSS_JOIN))
              .orElse(null);
      node.put(QUERY_SPEC_FROM, tableSourceNode);
    }

    return node;
  }

  @Override
  public SQLNode visitFrom_item(PGParser.From_itemContext ctx) {
    if (ctx.LEFT_PAREN() != null) return ctx.from_item(0).accept(this);
    if (ctx.from_primary() != null) return ctx.from_primary().accept(this);
    final SQLNode left = ctx.from_item(0).accept(this);
    final SQLNode right = ctx.from_item(1).accept(this);
    final SQLNode node = joined(left, right, parseJoinType(ctx));
    if (ctx.vex() != null) node.put(JOINED_ON, ctx.vex().accept(this));
    // TODO: USING
    return node;
  }

  @Override
  public SQLNode visitFrom_primary(PGParser.From_primaryContext ctx) {
    if (ctx.schema_qualified_name() != null) {
      final SQLNode node = newTableSource(SQLTableSource.Kind.SIMPLE);
      node.put(SIMPLE_TABLE, tableName(stringifyIdentifier(ctx.schema_qualified_name())));
      if (ctx.alias_clause() != null) node.put(SIMPLE_ALIAS, parseAlias(ctx.alias_clause()));
      return node;

    } else if (ctx.table_subquery() != null) {
      final SQLNode node = newTableSource(SQLTableSource.Kind.DERIVED);
      node.put(DERIVED_SUBQUERY, ctx.table_subquery().accept(this));
      if (ctx.alias_clause() != null) node.put(DERIVED_ALIAS, parseAlias(ctx.alias_clause()));
      return node;
    }
    return null;
  }

  @Override
  public SQLNode visitSelect_sublist(PGParser.Select_sublistContext ctx) {
    final SQLNode node = new SQLNode(SELECT_ITEM);
    node.put(SELECT_ITEM_EXPR, ctx.vex().accept(this));
    if (ctx.col_label() != null) node.put(SELECT_ITEM_ALIAS, stringifyIdentifier(ctx.col_label()));
    else if (ctx.id_token() != null)
      node.put(SELECT_ITEM_ALIAS, stringifyIdentifier(ctx.id_token()));
    return node;
  }

  @Override
  public SQLNode visitGrouping_element(PGParser.Grouping_elementContext ctx) {
    if (ctx.vex() != null) return ctx.vex().accept(this);
    else return null; // TODO
  }

  @Override
  public SQLNode visitVex(PGParser.VexContext ctx) {
    if (ctx.CAST_EXPRESSION() != null) {
      final SQLNode node = newExpr(SQLExpr.Kind.CAST);
      node.put(CAST_EXPR, ctx.vex(0).accept(this));
      node.put(CAST_TYPE, symbol(ctx.data_type().getText().toLowerCase()));
      return node;

    } else if (ctx.collate_identifier() != null) {
      final SQLNode node = newExpr(SQLExpr.Kind.COLLATE);
      node.put(COLLATE_EXPR, ctx.vex(0).accept(this));
      node.put(
          COLLATE_COLLATION, commonName(stringifyIdentifier(ctx.collate_identifier().collation)));
      return node;

    } else if (ctx.value_expression_primary() != null) {
      return ctx.value_expression_primary().accept(this);

    } else if (ctx.unary_operator != null) {
      return unary(ctx.vex(0).accept(this), UnaryOp.ofOp(ctx.unary_operator.getText()));

    } else if (ctx.binary_operator != null) {
      final SQLNode node =
          binary(
              ctx.vex(0).accept(this),
              ctx.vex(1).accept(this),
              ctx.EXP() != null ? BinaryOp.EXP : BinaryOp.ofOp(ctx.binary_operator.getText()));
      return ctx.NOT() != null ? unary(node, UnaryOp.NOT) : node;

    } else if (ctx.AT() != null) {
      return binary(ctx.vex(0).accept(this), ctx.vex(1).accept(this), BinaryOp.AT_TIME_ZONE);

    } else if (ctx.SIMILAR() != null) {
      return binary(ctx.vex(0).accept(this), ctx.vex(1).accept(this), BinaryOp.AT_TIME_ZONE);

    } else if (ctx.ISNULL() != null) {
      return binary(ctx.vex(0).accept(this), literal(LiteralType.NULL, null), BinaryOp.IS);

    } else if (ctx.NOTNULL() != null) {
      return unary(
          binary(ctx.vex(0).accept(this), literal(LiteralType.NULL, null), BinaryOp.IS),
          UnaryOp.NOT);

    } else if (ctx.IN() != null) {
      final SQLNode left = ctx.vex(0).accept(this);
      if (ctx.select_stmt_no_parens() != null)
        return binary(left, ctx.select_stmt_no_parens().accept(this), BinaryOp.IN_SUBQUERY);
      else {
        final var vexs = ctx.vex();
        final SQLNode tuple = newExpr(SQLExpr.Kind.TUPLE);
        tuple.put(TUPLE_EXPRS, listMap(this::visitVex, vexs.subList(1, vexs.size())));
        return binary(left, tuple, BinaryOp.IN_LIST);
      }

    } else if (ctx.BETWEEN() != null) {
      final SQLNode node = newExpr(SQLExpr.Kind.TERNARY);
      node.put(TERNARY_OP, TernaryOp.BETWEEN_AND);
      node.put(TERNARY_LEFT, ctx.vex(0).accept(this));
      node.put(TERNARY_MIDDLE, ctx.vex_b().accept(this));
      node.put(TERNARY_RIGHT, ctx.vex(1).accept(this));
      return node;

    } else if (ctx.IS() != null) {
      final SQLNode left = ctx.vex(0).accept(this);

      final SQLNode right;
      if (ctx.truth_value() != null)
        right = literal(LiteralType.BOOL, parseTruthValue(ctx.truth_value()));
      else if (ctx.NULL() != null) right = literal(LiteralType.NULL, null);
      else if (ctx.DOCUMENT() != null) right = symbol("DOCUMENT");
      else if (ctx.UNKNOWN() != null) right = literal(LiteralType.UNKNOWN, null);
      else if (ctx.vex(1) != null) right = ctx.vex(1).accept(this);
      // TODO: omit IS OF for now
      else return assertFalse();

      final BinaryOp op;
      if (ctx.DISTINCT() != null) op = BinaryOp.IS;
      else op = BinaryOp.IS_DISTINCT_FROM;

      return binary(left, right, op);

    } else if (ctx.op() != null) {
      // custom operator, omit it for now
      // TODO: custom operator
      return null;

    } else if (ctx.vex().size() == 1) {
      final SQLNode node = ctx.vex(0).accept(this);
      return ctx.indirection_list() == null
          ? node
          : indirection(node, parseIndirectionList(ctx.indirection_list()));

    } else if (ctx.vex().size() >= 1) {
      final SQLNode tuple = newExpr(SQLExpr.Kind.TUPLE);
      tuple.put(TUPLE_EXPRS, listMap(this::visitVex, ctx.vex()));
      return tuple;

    } else return assertFalse();
  }

  @Override
  public SQLNode visitVex_b(PGParser.Vex_bContext ctx) {
    if (ctx.CAST_EXPRESSION() != null) {
      final SQLNode node = newExpr(SQLExpr.Kind.CAST);
      node.put(CAST_EXPR, ctx.vex_b(0).accept(this));
      node.put(CAST_TYPE, symbol(ctx.data_type().getText().toLowerCase()));
      return node;

    } else if (ctx.value_expression_primary() != null) {
      return ctx.value_expression_primary().accept(this);

    } else if (ctx.unary_operator != null) {
      return unary(ctx.vex_b(0).accept(this), UnaryOp.ofOp(ctx.unary_operator.getText()));

    } else if (ctx.binary_operator != null) {
      final SQLNode node =
          binary(
              ctx.vex_b(0).accept(this),
              ctx.vex_b(1).accept(this),
              ctx.EXP() != null ? BinaryOp.EXP : BinaryOp.ofOp(ctx.binary_operator.getText()));
      return ctx.NOT() != null ? unary(node, UnaryOp.NOT) : node;

    } else if (ctx.IS() != null) {
      final SQLNode left = ctx.vex_b(0).accept(this);

      final SQLNode right;
      if (ctx.DOCUMENT() != null) right = symbol("DOCUMENT");
      else if (ctx.UNKNOWN() != null) right = literal(LiteralType.UNKNOWN, null);
      else if (ctx.vex_b(1) != null) right = ctx.vex_b(1).accept(this);
      // TODO: omit IS OF for now
      else return assertFalse();

      final BinaryOp op;
      if (ctx.DISTINCT() != null) op = BinaryOp.IS;
      else op = BinaryOp.IS_DISTINCT_FROM;

      return binary(left, right, op);

    } else if (ctx.op() != null) {
      // custom operator, omit it for now
      // TODO: custom operator
      return null;

    } else if (ctx.vex().size() == 1) {
      final SQLNode node = ctx.vex(0).accept(this);
      return ctx.indirection_list() == null
          ? node
          : indirection(node, parseIndirectionList(ctx.indirection_list()));

    } else if (ctx.vex().size() >= 1) {
      final SQLNode tuple = newExpr(SQLExpr.Kind.TUPLE);
      tuple.put(TUPLE_EXPRS, listMap(this::visitVex, ctx.vex()));
      return tuple;

    } else return assertFalse();
  }

  @Override
  public SQLNode visitValue_expression_primary(PGParser.Value_expression_primaryContext ctx) {
    if (ctx.unsigned_value_specification() != null)
      return parseUnsignedLiteral(ctx.unsigned_value_specification());
    else if (ctx.NULL() != null) return literal(LiteralType.NULL, null);
    else if (ctx.MULTIPLY() != null) return wildcard();
    else if (ctx.EXISTS() != null) {
      final SQLNode node = newExpr(SQLExpr.Kind.EXISTS);
      node.put(EXISTS_SUBQUERY, ctx.table_subquery().accept(this));
      return node;

    } else if (ctx.select_stmt_no_parens() != null) {
      final SQLNode queryExpr = newExpr(SQLExpr.Kind.QUERY_EXPR);
      queryExpr.put(QUERY_EXPR_QUERY, ctx.select_stmt_no_parens().accept(this));
      return ctx.indirection_list() == null
          ? queryExpr
          : indirection(queryExpr, parseIndirectionList(ctx.indirection_list()));

    } else if (ctx.case_expression() != null) {
      return ctx.case_expression().accept(this);

    } else if (ctx.comparison_mod() != null) {
      return ctx.comparison_mod().accept(this);

    } else if (ctx.indirection_var() != null) {
      return ctx.indirection_var().accept(this);

    } else if (ctx.type_coercion() != null) {
      return ctx.type_coercion().accept(this);

    } else if (ctx.datetime_overlaps() != null) {
      return ctx.datetime_overlaps().accept(this);

    } else return assertFalse();
  }

  @Override
  public SQLNode visitIndirection_var(PGParser.Indirection_varContext ctx) {
    if (ctx.dollar_number() != null) {
      final SQLNode param = parseParam(ctx.dollar_number());
      if (ctx.indirection_list() != null) {
        final List<SQLNode> indirections = parseIndirectionList(ctx.indirection_list());
        final SQLNode indirection = newExpr(SQLExpr.Kind.INDIRECTION);
        indirection.put(INDIRECTION_EXPR, param);
        indirection.put(INDIRECTION_COMPS, indirections);
        return indirection;
      } else return param;

    } else if (ctx.identifier() != null) {
      final String identifier = stringifyIdentifier(ctx.identifier());
      if (ctx.indirection_list() == null) return columnRef(null, identifier);
      else {
        final List<SQLNode> indirections = parseIndirectionList(ctx.indirection_list());
        return buildIndirection(identifier, indirections);
      }

    } else return assertFalse();
  }

  @Override
  public SQLNode visitIndirection(PGParser.IndirectionContext ctx) {
    final SQLNode node = newExpr(SQLExpr.Kind.INDIRECTION_COMP);
    if (ctx.col_label() != null) {
      node.put(INDIRECTION_COMP_START, symbol(ctx.col_label().getText()));

    } else if (ctx.COLON() != null) {
      final var start = ctx.start;
      final var end = ctx.end;
      if (start != null) node.put(INDIRECTION_COMP_START, start.accept(this));
      if (end != null) node.put(INDIRECTION_COMP_END, end.accept(this));

    } else {
      node.put(INDIRECTION_COMP_START, ctx.vex(0).accept(this));
    }

    return node;
  }

  @Override
  public SQLNode visitCase_expression(PGParser.Case_expressionContext ctx) {
    final SQLNode _case = newExpr(SQLExpr.Kind.CASE);
    if (ctx.condition != null) _case.put(CASE_COND, ctx.condition.accept(this));
    if (ctx.otherwise != null) _case.put(CASE_ELSE, ctx.otherwise.accept(this));
    final var whens = ctx.when;
    final var thens = ctx.then;
    final List<SQLNode> whenNodes = new ArrayList<>(whens.size());
    for (int i = 0; i < whens.size(); i++) {
      final var when = whens.get(i);
      final var then = thens.get(i);
      final SQLNode whenNode = newExpr(SQLExpr.Kind.WHEN);
      whenNode.put(WHEN_COND, when.accept(this));
      whenNode.put(WHEN_EXPR, then.accept(this));
    }

    _case.put(CASE_WHENS, whenNodes);
    return _case;
  }

  @Override
  public SQLNode visitComparison_mod(PGParser.Comparison_modContext ctx) {
    final SQLNode node = newExpr(SQLExpr.Kind.COMPARISON_MOD);

    final SubqueryOption option;
    if (ctx.ALL() != null) option = SubqueryOption.ALL;
    else if (ctx.ANY() != null) option = SubqueryOption.ANY;
    else if (ctx.SOME() != null) option = SubqueryOption.SOME;
    else option = null;

    node.put(COMPARISON_MOD_OPTION, option);
    if (ctx.vex() != null) node.put(COMPARISON_MOD_EXPR, ctx.vex().accept(this));
    else if (ctx.select_stmt_no_parens() != null)
      node.put(COMPARISON_MOD_EXPR, ctx.select_stmt_no_parens().accept(this));

    return node;
  }

  @Override
  public SQLNode visitArray_expression(PGParser.Array_expressionContext ctx) {
    if (ctx.array_elements() != null) return ctx.array_elements().accept(this);
    else if (ctx.table_subquery() != null) {
      final SQLNode node = newExpr(SQLExpr.Kind.ARRAY);
      node.put(ARRAY_ELEMENTS, Collections.singletonList(ctx.table_subquery().accept(this)));
      return node;
    }
    return assertFalse();
  }

  @Override
  public SQLNode visitArray_elements(PGParser.Array_elementsContext ctx) {
    final SQLNode node = newExpr(SQLExpr.Kind.ARRAY);
    final List<SQLNode> elements = listMap(this::visitArray_element, ctx.array_element());
    node.put(ARRAY_ELEMENTS, elements);
    return node;
  }

  @Override
  public SQLNode visitArray_element(PGParser.Array_elementContext ctx) {
    if (ctx.vex() != null) return ctx.vex().accept(this);
    else if (ctx.array_elements() != null) return ctx.array_elements().accept(this);
    else return assertFalse();
  }

  @Override
  public SQLNode visitType_coercion(PGParser.Type_coercionContext ctx) {
    final SQLNode node = newExpr(SQLExpr.Kind.TYPE_COERCION);
    node.put(TYPE_COERCION_TYPE, parseDataType(ctx.data_type()));
    node.put(TYPE_COERCION_STRING, stringifyText(ctx.character_string()));
    return node;
  }

  @Override
  public SQLNode visitDatetime_overlaps(PGParser.Datetime_overlapsContext ctx) {
    final SQLNode node = newExpr(SQLExpr.Kind.DATETIME_OVERLAP);
    node.put(DATETIME_OVERLAP_LEFT_START, ctx.vex(0).accept(this));
    node.put(DATETIME_OVERLAP_LEFT_END, ctx.vex(1).accept(this));
    node.put(DATETIME_OVERLAP_RIGHT_START, ctx.vex(2).accept(this));
    node.put(DATETIME_OVERLAP_RIGHT_END, ctx.vex(3).accept(this));
    return node;
  }

  private SQLNode addAfterOp(SQLNode node, PGParser.After_opsContext ctx) {
    if (ctx.LIMIT() != null && ctx.ALL() == null) node.put(QUERY_LIMIT, ctx.vex().accept(this));
    else if (ctx.OFFSET() != null) node.put(QUERY_OFFSET, ctx.vex().accept(this));
    else if (ctx.orderby_clause() != null) {
      node.put(
          QUERY_ORDER_BY,
          listMap(
              this::visitSort_specifier,
              ctx.orderby_clause().sort_specifier_list().sort_specifier()));
    }
    return node;
  }

  private void addConstraint(SQLNode node, PGParser.Constr_bodyContext ctx) {
    if (ctx.CHECK() != null) node.flag(COLUMN_DEF_CONS, ConstraintType.CHECK);
    else if (ctx.NOT() != null) node.flag(COLUMN_DEF_CONS, ConstraintType.NOT_NULL);
    else if (ctx.UNIQUE() != null) node.flag(COLUMN_DEF_CONS, UNIQUE);
    else if (ctx.PRIMARY() != null) node.flag(COLUMN_DEF_CONS, ConstraintType.PRIMARY);
    else if (ctx.DEFAULT() != null) node.flag(COLUMN_DEF_DEFAULT);
    else if (ctx.GENERATED() != null) node.flag(COLUMN_DEF_GENERATED);
    else if (ctx.identity_body() != null) node.flag(COLUMN_DEF_AUTOINCREMENT);
    else if (ctx.REFERENCES() != null) {
      final SQLNode ref = new SQLNode(REFERENCES);
      ref.put(REFERENCES_TABLE, tableName(stringifyIdentifier(ctx.schema_qualified_name())));
      if (ctx.ref != null) ref.put(REFERENCES_COLUMNS, columnNames(ctx.ref.names_references()));

      node.put(COLUMN_DEF_REF, ref);
    }
  }

  private List<SQLNode> parseIndirectionList(PGParser.Indirection_listContext ctx) {
    final List<SQLNode> indirections = listMap(this::visitIndirection, ctx.indirection());
    if (ctx.MULTIPLY() != null) {
      final SQLNode node = newExpr(SQLExpr.Kind.INDIRECTION_COMP);
      node.put(INDIRECTION_COMP_START, wildcard());
      indirections.add(node);
    }
    return indirections;
  }
}
