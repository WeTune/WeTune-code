package sjtu.ipads.wtune.sqlparser.mysql;

import org.antlr.v4.runtime.Token;
import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.common.utils.Commons;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.*;
import sjtu.ipads.wtune.sqlparser.ast.internal.ASTNodeFactory;
import sjtu.ipads.wtune.sqlparser.mysql.internal.MySQLParser;
import sjtu.ipads.wtune.sqlparser.mysql.internal.MySQLParserBaseVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.Commons.assertFalse;
import static sjtu.ipads.wtune.common.utils.Commons.unquoted;
import static sjtu.ipads.wtune.common.utils.FuncUtils.*;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.TableSourceFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.IndexType.FULLTEXT;
import static sjtu.ipads.wtune.sqlparser.ast.constants.IndexType.SPATIAL;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.TableSourceKind.*;
import static sjtu.ipads.wtune.sqlparser.mysql.MySQLASTHelper.*;

public class MySQLASTBuilder extends MySQLParserBaseVisitor<ASTNode> implements ASTNodeFactory {
  @Override
  protected ASTNode aggregateResult(ASTNode aggregate, ASTNode nextResult) {
    return Commons.coalesce(aggregate, nextResult);
  }

  @Override
  public ASTNode visitAlterTable(MySQLParser.AlterTableContext ctx) {
    final ASTNode alter = newNode(ALTER_TABLE);

    final ASTNode tableName = visitTableRef(ctx.tableRef());
    final List<ASTNode> actions =
        listMap(
            ctx.alterTableActions().alterCommandList().alterList().alterListItem(),
            this::visitAlterListItem);

    alter.set(ALTER_TABLE_NAME, tableName);
    alter.set(ALTER_TABLE_ACTIONS, actions);

    return alter;
  }

  @Override
  public ASTNode visitAlterListItem(MySQLParser.AlterListItemContext ctx) {
    if (ctx.tableConstraintDef() != null) {
      final ASTNode action = newNode(ALTER_TABLE_ACTION);

      action.set(ALTER_TABLE_ACTION_NAME, "add_constraint");
      action.set(ALTER_TABLE_ACTION_PAYLOAD, visitTableConstraintDef(ctx.tableConstraintDef()));
      return action;

    } else if (ctx.MODIFY_SYMBOL() != null) {
      final ASTNode action = newNode(ALTER_TABLE_ACTION);
      final ASTNode colDef = newNode(COLUMN_DEF);
      final ASTNode colName = newNode(COLUMN_NAME);

      colName.set(COLUMN_NAME_COLUMN, stringifyIdentifier(ctx.columnInternalRef().identifier()));
      colDef.set(COLUMN_DEF_NAME, colName);
      parseFieldDefinition(ctx.fieldDefinition(), colDef);

      action.set(ALTER_TABLE_ACTION_NAME, "modify_column");
      action.set(ALTER_TABLE_ACTION_PAYLOAD, colDef);
      return action;
    }
    return null;
  }

  @Override
  public ASTNode visitTableName(MySQLParser.TableNameContext tableName) {
    return MySQLASTHelper.tableName(
        this, tableName.qualifiedIdentifier(), tableName.dotIdentifier());
  }

  @Override
  public ASTNode visitTableRef(MySQLParser.TableRefContext tableName) {
    return MySQLASTHelper.tableName(
        this, tableName.qualifiedIdentifier(), tableName.dotIdentifier());
  }

  @Override
  public ASTNode visitColumnName(MySQLParser.ColumnNameContext ctx) {
    final ASTNode node = newNode(NodeType.COLUMN_NAME);
    final String schema, table, column;

    if (ctx.identifier() != null) {
      schema = null;
      table = null;
      column = stringifyIdentifier(ctx.identifier());

    } else if (ctx.fieldIdentifier() != null) {
      final String[] triple = stringifyIdentifier(ctx.fieldIdentifier());
      schema = triple[0];
      table = triple[1];
      column = triple[2];

    } else {
      return assertFalse();
    }

    node.set(COLUMN_NAME_SCHEMA, schema);
    node.set(COLUMN_NAME_TABLE, table);
    node.set(COLUMN_NAME_COLUMN, column);

    return node;
  }

  @Override
  public ASTNode visitCreateTable(MySQLParser.CreateTableContext ctx) {
    if (ctx.tableElementList() == null) return null;

    final ASTNode node = newNode(CREATE_TABLE);
    node.set(CREATE_TABLE_NAME, visitTableName(ctx.tableName()));

    final List<ASTNode> columnDefs = new ArrayList<>();
    final List<ASTNode> contraintDefs = new ArrayList<>();

    for (var element : ctx.tableElementList().tableElement()) {
      final var colDefs = element.columnDefinition();
      final var consDefs = element.tableConstraintDef();

      if (colDefs != null) columnDefs.add(visitColumnDefinition(colDefs));
      else if (consDefs != null) contraintDefs.add(visitTableConstraintDef(consDefs));
      else return assertFalse();
    }

    node.set(CREATE_TABLE_COLUMNS, columnDefs);
    node.set(CREATE_TABLE_CONSTRAINTS, contraintDefs);

    final var tableOptions = ctx.createTableOptions();
    if (tableOptions != null)
      for (var option : tableOptions.createTableOption())
        if (option.ENGINE_SYMBOL() != null) {
          node.set(CREATE_TABLE_ENGINE, stringifyText(option.engineRef().textOrIdentifier()));
          break;
        }

    return node;
  }

  @Override
  public ASTNode visitColumnDefinition(MySQLParser.ColumnDefinitionContext ctx) {
    final ASTNode node = newNode(NodeType.COLUMN_DEF);
    node.set(COLUMN_DEF_NAME, visitColumnName(ctx.columnName()));

    parseFieldDefinition(ctx.fieldDefinition(), node);

    final var checkOrRef = ctx.checkOrReferences();
    if (checkOrRef != null)
      if (checkOrRef.references() != null)
        node.set(COLUMN_DEF_REF, visitReferences(checkOrRef.references()));
      else if (checkOrRef.checkConstraint() != null) node.flag(COLUMN_DEF_CONS, CHECK);

    return node;
  }

  @Override
  public ASTNode visitReferences(MySQLParser.ReferencesContext ctx) {
    final ASTNode node = newNode(NodeType.REFERENCES);

    node.set(REFERENCES_TABLE, visitTableRef(ctx.tableRef()));

    final var idList = ctx.identifierListWithParentheses();
    if (idList != null) {
      final var ids = idList.identifierList().identifier();
      final List<ASTNode> columns = new ArrayList<>(ids.size());

      for (var id : ids) {
        final ASTNode columnRef = newNode(NodeType.COLUMN_NAME);
        columnRef.set(COLUMN_NAME_COLUMN, stringifyIdentifier(id));
        columns.add(columnRef);
      }
      node.set(REFERENCES_COLUMNS, columns);
    }

    return node;
  }

  @Override
  public ASTNode visitTableConstraintDef(MySQLParser.TableConstraintDefContext ctx) {
    final ASTNode node = newNode(NodeType.INDEX_DEF);
    final Token type = ctx.type;
    if (type == null) return null;

    final var indexNameAndType = ctx.indexNameAndType();
    final var indexName = ctx.indexName();
    final var indexOptions = ctx.indexOption();

    final ConstraintType c;
    final IndexType t;
    final String name;
    switch (type.getText().toLowerCase()) {
      case "key":
      case "index":
        c = null;
        t = Commons.coalesce(parseIndexType(indexNameAndType), parseIndexOption(indexOptions));
        name = stringifyIndexName(indexNameAndType);
        break;

      case "fulltext":
        c = null;
        t = FULLTEXT;
        name = stringifyIndexName(indexName);
        break;

      case "spatial":
        c = null;
        t = SPATIAL;
        name = stringifyIndexName(indexName);
        break;

      case "primary":
        c = PRIMARY;
        t = Commons.coalesce(parseIndexType(indexNameAndType), parseIndexOption(indexOptions));
        name = stringifyIndexName(indexNameAndType);
        break;

      case "unique":
        c = UNIQUE;
        t = Commons.coalesce(parseIndexType(indexNameAndType), parseIndexOption(indexOptions));
        name = stringifyIndexName(indexNameAndType);
        break;

      case "foreign":
        c = FOREIGN;
        t = null;
        name = stringifyIndexName(indexName);
        break;

      default:
        return assertFalse();
    }

    node.set(INDEX_DEF_CONS, c);
    node.set(INDEX_DEF_TYPE, t);
    node.set(INDEX_DEF_NAME, name);

    final var keyListVariants = ctx.keyListVariants();
    final var keyList = keyListVariants != null ? keyListVariants.keyList() : ctx.keyList();
    final List<ASTNode> keys;

    if (keyList != null) {
      keys = listMap(keyList.keyPart(), this::visitKeyPart);
    } else if (keyListVariants != null) {
      keys =
          listMap(
              keyListVariants.keyListWithExpression().keyPartOrExpression(),
              this::visitKeyPartOrExpression);
    } else {
      return assertFalse();
    }

    node.set(INDEX_DEF_KEYS, keys);

    final var references = ctx.references();
    if (references != null) node.set(INDEX_DEF_REFS, visitReferences(references));

    return node;
  }

  @Override
  public ASTNode visitKeyPart(MySQLParser.KeyPartContext ctx) {
    final ASTNode node = newNode(NodeType.KEY_PART);
    node.set(KEY_PART_COLUMN, stringifyIdentifier(ctx.identifier()));
    node.set(KEY_PART_DIRECTION, parseDirection(ctx.direction()));
    if (ctx.fieldLength() != null) node.set(KEY_PART_LEN, fieldLength2Int(ctx.fieldLength()));
    return node;
  }

  @Override
  public ASTNode visitKeyPartOrExpression(MySQLParser.KeyPartOrExpressionContext ctx) {
    if (ctx.keyPart() != null) return visitKeyPart(ctx.keyPart());
    else if (ctx.exprWithParentheses() != null) {
      final ASTNode node = newNode(KEY_PART);

      node.set(KEY_PART_EXPR, toExpr(ctx.exprWithParentheses().expr()));
      if (ctx.direction() != null) node.set(KEY_PART_DIRECTION, parseDirection(ctx.direction()));

      return node;
    } else return null;
  }

  @Override
  public ASTNode visitQueryExpression(MySQLParser.QueryExpressionContext ctx) {
    // currently WITH is not supported
    final ASTNode query = newNode(QUERY);
    if (ctx.withClause() != null) return null;

    if (ctx.queryExpressionBody() != null) {
      query.set(QUERY_BODY, ctx.queryExpressionBody().accept(this));
    } else if (ctx.queryExpressionParens() != null) {
      query.set(QUERY_BODY, ctx.queryExpressionParens().accept(this));
    }

    final var orderByClause = ctx.orderClause();
    if (orderByClause != null) query.set(QUERY_ORDER_BY, toOrderItems(orderByClause));

    final var limitClause = ctx.limitClause();
    if (limitClause != null) {
      final var limitOptions = limitClause.limitOptions().limitOption();
      if (limitOptions.size() == 1) {
        query.set(QUERY_LIMIT, limitOptions.get(0).accept(this));

      } else if (limitOptions.size() == 2) {
        if (limitClause.limitOptions().OFFSET_SYMBOL() != null) {
          query.set(QUERY_OFFSET, limitOptions.get(1).accept(this));
          query.set(QUERY_LIMIT, limitOptions.get(0).accept(this));
        } else {
          query.set(QUERY_OFFSET, limitOptions.get(0).accept(this));
          query.set(QUERY_LIMIT, limitOptions.get(1).accept(this));
        }
      }
    }

    return query;
  }

  @Override
  public ASTNode visitLimitOption(MySQLParser.LimitOptionContext ctx) {
    if (ctx.PARAM_MARKER() != null) return ASTNode.expr(ExprKind.PARAM_MARKER);
    if (ctx.ULONGLONG_NUMBER() != null)
      return literal(LiteralType.LONG, Long.parseLong(ctx.ULONGLONG_NUMBER().getText()));
    if (ctx.LONG_NUMBER() != null)
      return literal(LiteralType.LONG, Long.parseLong(ctx.LONG_NUMBER().getText()));
    if (ctx.INT_NUMBER() != null)
      return literal(LiteralType.INTEGER, Integer.parseInt(ctx.INT_NUMBER().getText()));
    if (ctx.identifier() != null) {
      final ASTNode variable = newNode(VARIABLE);
      variable.set(VARIABLE_NAME, stringifyIdentifier(ctx.identifier()));
    }
    return null;
  }

  @Override
  public ASTNode visitQueryExpressionBody(MySQLParser.QueryExpressionBodyContext ctx) {
    if (ctx.UNION_SYMBOL() == null) return ctx.querySpecification().accept(this);

    final ASTNode left, right;
    if (ctx.queryExpressionBody() != null) {
      left = ctx.queryExpressionBody().accept(this);
      if (ctx.querySpecification() != null) right = ctx.querySpecification().accept(this);
      else right = ctx.queryExpressionParens(0).accept(this);

    } else if (ctx.queryExpressionParens(0) != null) {
      left = ctx.queryExpressionParens(0).accept(this);
      if (ctx.querySpecification() != null) right = ctx.querySpecification().accept(this);
      else right = ctx.queryExpressionParens(1).accept(this);
    } else return assertFalse();

    final SetOperationOption option =
        ctx.unionOption() == null
            ? null
            : SetOperationOption.valueOf(ctx.unionOption().getText().toUpperCase());

    final ASTNode node = newNode(SET_OP);
    node.set(SET_OP_TYPE, SetOperation.UNION);
    node.set(SET_OP_LEFT, wrapAsQuery(this, left));
    node.set(SET_OP_RIGHT, wrapAsQuery(this, right));
    node.set(SET_OP_OPTION, option);

    return node;
  }

  @Override
  public ASTNode visitQuerySpecification(MySQLParser.QuerySpecificationContext ctx) {
    final ASTNode node = newNode(QUERY_SPEC);

    if (ctx.selectOption() != null
        && find(ctx.selectOption(), it -> "DISTINCT".equalsIgnoreCase(it.getText())) != null)
      node.flag(QUERY_SPEC_DISTINCT);

    final var selectItemList = ctx.selectItemList();
    final List<ASTNode> items = new ArrayList<>(selectItemList.selectItem().size() + 1);
    if (selectItemList.MULT_OPERATOR() != null) items.add(selectItem(newNode(WILDCARD), null));
    items.addAll(listMap(selectItemList.selectItem(), this::visitSelectItem));
    node.set(QUERY_SPEC_SELECT_ITEMS, items);

    final var fromClause = ctx.fromClause();
    if (fromClause != null) {
      final ASTNode from = visitFromClause(fromClause);
      if (from != null) node.set(QUERY_SPEC_FROM, from);
    }

    final var whereClause = ctx.whereClause();
    if (whereClause != null) node.set(QUERY_SPEC_WHERE, toExpr(whereClause.expr()));

    final var groupByClause = ctx.groupByClause();
    if (groupByClause != null) {
      final OLAPOption olapOption = parseOLAPOption(groupByClause.olapOption());
      if (olapOption != null) node.set(QUERY_SPEC_OLAP_OPTION, olapOption);
      node.set(QUERY_SPEC_GROUP_BY, toGroupItems(groupByClause.orderList()));
    }

    final var havingClause = ctx.havingClause();
    if (havingClause != null) node.set(QUERY_SPEC_HAVING, toExpr(havingClause.expr()));

    final var windowClause = ctx.windowClause();
    if (windowClause != null)
      node.set(
          QUERY_SPEC_WINDOWS,
          listMap(windowClause.windowDefinition(), this::visitWindowDefinition));

    return node;
  }

  @Override
  public ASTNode visitFromClause(MySQLParser.FromClauseContext ctx) {
    if (ctx.DUAL_SYMBOL() != null) return null;

    return ctx.tableReferenceList().tableReference().stream()
        .map(this::visitTableReference)
        .reduce((l, r) -> joined(l, r, JoinType.CROSS_JOIN))
        .orElse(null);
  }

  @Override
  public ASTNode visitSingleTable(MySQLParser.SingleTableContext ctx) {
    final ASTNode node = newNode(SIMPLE_SOURCE);
    node.set(SIMPLE_TABLE, visitTableRef(ctx.tableRef()));

    if (ctx.usePartition() != null) {
      final var identifiers =
          ctx.usePartition().identifierListWithParentheses().identifierList().identifier();
      node.set(SIMPLE_PARTITIONS, listMap(identifiers, MySQLASTHelper::stringifyIdentifier));
    }

    if (ctx.tableAlias() != null)
      node.set(SIMPLE_ALIAS, stringifyIdentifier(ctx.tableAlias().identifier()));

    if (ctx.indexHintList() != null)
      node.set(SIMPLE_HINTS, listMap(ctx.indexHintList().indexHint(), this::visitIndexHint));

    return node;
  }

  @Override
  public ASTNode visitDerivedTable(MySQLParser.DerivedTableContext ctx) {
    final ASTNode node = newNode(DERIVED_SOURCE);

    if (ctx.LATERAL_SYMBOL() != null) node.flag(DERIVED_LATERAL);

    node.set(DERIVED_SUBQUERY, visitSubquery(ctx.subquery()));

    if (ctx.tableAlias() != null)
      node.set(DERIVED_ALIAS, stringifyIdentifier(ctx.tableAlias().identifier()));

    if (ctx.columnInternalRefList() != null) {
      final List<String> internalRefs =
          ctx.columnInternalRefList().columnInternalRef().stream()
              .map(MySQLParser.ColumnInternalRefContext::identifier)
              .map(MySQLASTHelper::stringifyIdentifier)
              .collect(Collectors.toList());
      node.set(DERIVED_INTERNAL_REFS, internalRefs);
    }

    return node;
  }

  @Override
  public ASTNode visitTableRefList(MySQLParser.TableRefListContext ctx) {
    return ctx.tableRef().stream()
        .map(this::visitTableRef)
        .reduce((left, right) -> joined(left, right, JoinType.CROSS_JOIN))
        .orElse(null);
  }

  @Override
  public ASTNode visitTableReference(MySQLParser.TableReferenceContext ctx) {
    final ASTNode left = ctx.tableFactor().accept(this);
    final var joinedTables = ctx.joinedTable();
    if (joinedTables == null || joinedTables.isEmpty()) return left;
    return joinedTables.stream()
        .map(this::visitJoinedTable)
        .reduce(
            left,
            (l, r) -> {
              r.set(JOINED_LEFT, l);
              return r;
            });
  }

  @Override
  public ASTNode visitJoinedTable(MySQLParser.JoinedTableContext ctx) {
    final ASTNode node = newNode(JOINED_SOURCE);
    final JoinType joinType =
        Commons.coalesce(
            parseJoinType(ctx.innerJoinType()),
            parseJoinType(ctx.outerJoinType()),
            parseJoinType(ctx.naturalJoinType()));

    assert joinType != null;

    node.set(JOINED_TYPE, joinType);

    if (ctx.expr() != null) node.set(JOINED_ON, toExpr(ctx.expr()));
    if (ctx.identifierListWithParentheses() != null) {
      final var identifiers = ctx.identifierListWithParentheses().identifierList().identifier();
      node.set(JOINED_USING, listMap(identifiers, MySQLASTHelper::stringifyIdentifier));
    }

    final ASTNode right;
    if (ctx.tableReference() != null) right = ctx.tableReference().accept(this);
    else if (ctx.tableFactor() != null) right = ctx.tableFactor().accept(this);
    else return assertFalse();

    node.set(JOINED_RIGHT, right);

    return node;
  }

  @Override
  public ASTNode visitIndexHint(MySQLParser.IndexHintContext ctx) {
    final ASTNode node = newNode(INDEX_HINT);
    node.set(INDEX_HINT_TYPE, parseIndexHintType(ctx));

    final IndexHintTarget target = parseIndexHintTarget(ctx.indexHintClause());
    if (target != null) node.set(INDEX_HINT_TARGET, target);

    final var indexList = ctx.indexList();
    if (indexList != null) {
      node.set(
          INDEX_HINT_NAMES,
          listMap(indexList.indexListElement(), MySQLASTHelper::parseIndexListElement));
    }

    return node;
  }

  @Override
  public ASTNode visitSelectItem(MySQLParser.SelectItemContext ctx) {
    final ASTNode item = newNode(SELECT_ITEM);
    if (ctx.tableWild() != null) item.set(SELECT_ITEM_EXPR, visitTableWild(ctx.tableWild()));

    if (ctx.expr() != null) item.set(SELECT_ITEM_EXPR, toExpr(ctx.expr()));

    if (ctx.selectAlias() != null) {
      final var selectAlias = ctx.selectAlias();
      final String alias =
          selectAlias.identifier() != null
              ? stringifyIdentifier(selectAlias.identifier())
              : stringifyText(selectAlias.textStringLiteral());
      item.set(SELECT_ITEM_ALIAS, alias);
    }

    return item;
  }

  @Override
  public ASTNode visitTableWild(MySQLParser.TableWildContext ctx) {
    final ASTNode table = newNode(TABLE_NAME);

    final List<MySQLParser.IdentifierContext> ids = ctx.identifier();
    final String id0 = stringifyIdentifier(ids.get(0));
    final String id1 = ids.size() >= 2 ? stringifyIdentifier(ids.get(1)) : null;

    final String schemaName = id1 != null ? id0 : null;
    final String tableName = id1 != null ? id1 : id0;

    table.set(TABLE_NAME_SCHEMA, schemaName);
    table.set(TABLE_NAME_TABLE, tableName);

    return wildcard(table);
  }

  @Override
  public ASTNode visitExprIs(MySQLParser.ExprIsContext ctx) {
    final ASTNode left = ctx.boolPri().accept(this);
    if (ctx.IS_SYMBOL() == null) return left;

    final ASTNode right;
    if (ctx.UNKNOWN_SYMBOL() != null) right = literal(LiteralType.UNKNOWN, null);
    else if (ctx.TRUE_SYMBOL() != null) right = literal(LiteralType.BOOL, true);
    else if (ctx.FALSE_SYMBOL() != null) right = literal(LiteralType.BOOL, false);
    else return assertFalse();

    final ASTNode node = binary(left, right, BinaryOp.IS);

    if (ctx.notRule() == null) return node;
    else return unary(node, UnaryOp.NOT);
  }

  @Override
  public ASTNode visitExprNot(MySQLParser.ExprNotContext ctx) {
    final ASTNode node = newNode(UNARY);
    node.set(UNARY_OP, UnaryOp.NOT);
    node.set(UNARY_EXPR, ctx.expr().accept(this));
    return node;
  }

  @Override
  public ASTNode visitExprAnd(MySQLParser.ExprAndContext ctx) {
    return binary(ctx.expr(0).accept(this), ctx.expr(1).accept(this), BinaryOp.AND);
  }

  @Override
  public ASTNode visitExprOr(MySQLParser.ExprOrContext ctx) {
    return binary(ctx.expr(0).accept(this), ctx.expr(1).accept(this), BinaryOp.OR);
  }

  @Override
  public ASTNode visitExprXor(MySQLParser.ExprXorContext ctx) {
    return binary(ctx.expr(0).accept(this), ctx.expr(1).accept(this), BinaryOp.XOR_SYMBOL);
  }

  @Override
  public ASTNode visitPrimaryExprIsNull(MySQLParser.PrimaryExprIsNullContext ctx) {
    final ASTNode node =
        binary(ctx.boolPri().accept(this), literal(LiteralType.NULL, null), BinaryOp.IS);

    if (ctx.notRule() == null) return node;
    else {
      final ASTNode not = newNode(UNARY);
      not.set(UNARY_OP, UnaryOp.NOT);
      not.set(UNARY_EXPR, node);
      return not;
    }
  }

  @Override
  public ASTNode visitPrimaryExprCompare(MySQLParser.PrimaryExprCompareContext ctx) {
    return binary(
        ctx.boolPri().accept(this),
        visitPredicate(ctx.predicate()),
        BinaryOp.ofOp(ctx.compOp().getText()));
  }

  @Override
  public ASTNode visitPrimaryExprAllAny(MySQLParser.PrimaryExprAllAnyContext ctx) {
    final ASTNode node =
        binary(
            ctx.boolPri().accept(this),
            wrapAsQueryExpr(this, ctx.subquery().accept(this)),
            BinaryOp.ofOp(ctx.compOp().getText()));

    if (ctx.ALL_SYMBOL() != null) node.set(BINARY_SUBQUERY_OPTION, SubqueryOption.ALL);
    else if (ctx.ANY_SYMBOL() != null) node.set(BINARY_SUBQUERY_OPTION, SubqueryOption.ANY);

    return node;
  }

  @Override
  public ASTNode visitPredicate(MySQLParser.PredicateContext ctx) {
    final ASTNode bitExpr = visitBitExpr(ctx.bitExpr(0));
    if (ctx.predicateOperations() != null) {
      ASTNode node = null;
      final var predicateOp = ctx.predicateOperations();

      if (predicateOp instanceof MySQLParser.PredicateExprInContext) {
        final var inExpr = (MySQLParser.PredicateExprInContext) predicateOp;
        final var subquery = inExpr.subquery();

        final BinaryOp op;
        final ASTNode right;
        if (subquery != null) {
          op = BinaryOp.IN_SUBQUERY;
          right = wrapAsQueryExpr(this, subquery.accept(this));

        } else {
          final ASTNode tuple = newNode(TUPLE);
          tuple.set(TUPLE_EXPRS, toExprs(inExpr.exprList()));
          op = BinaryOp.IN_LIST;
          right = tuple;
        }

        node = binary(bitExpr, right, op);
      }

      if (predicateOp instanceof MySQLParser.PredicateExprBetweenContext) {
        node = newNode(TERNARY);
        final var betweenExpr = (MySQLParser.PredicateExprBetweenContext) predicateOp;

        node.set(TERNARY_OP, TernaryOp.BETWEEN_AND);
        node.set(TERNARY_LEFT, bitExpr);
        node.set(TERNARY_MIDDLE, betweenExpr.bitExpr().accept(this));
        node.set(TERNARY_RIGHT, betweenExpr.predicate().accept(this));
      }

      if (predicateOp instanceof MySQLParser.PredicateExprLikeContext)
        node =
            binary(
                bitExpr,
                ((MySQLParser.PredicateExprLikeContext) predicateOp).simpleExpr(0).accept(this),
                BinaryOp.LIKE);

      if (predicateOp instanceof MySQLParser.PredicateExprRegexContext)
        node =
            binary(
                bitExpr,
                ((MySQLParser.PredicateExprRegexContext) predicateOp).bitExpr().accept(this),
                BinaryOp.REGEXP);

      if (node != null) {
        if (ctx.notRule() != null) node = unary(node, UnaryOp.NOT);
        return node;
      } else return assertFalse();
    }

    if (ctx.MEMBER_SYMBOL() != null)
      return binary(
          bitExpr, ctx.simpleExprWithParentheses().simpleExpr().accept(this), BinaryOp.MEMBER_OF);

    if (ctx.SOUNDS_SYMBOL() != null)
      return binary(bitExpr, ctx.bitExpr(1).accept(this), BinaryOp.SOUNDS_LIKE);

    return bitExpr;
  }

  @Override
  public ASTNode visitBitExpr(MySQLParser.BitExprContext ctx) {
    if (ctx.simpleExpr() != null) return ctx.simpleExpr().accept(this);

    final ASTNode right;
    if (ctx.interval() != null) {
      right = newNode(INTERVAL);
      right.set(INTERVAL_EXPR, toExpr(ctx.expr()));
      right.set(INTERVAL_UNIT, parseIntervalUnit(ctx.interval()));

    } else right = ctx.bitExpr(1).accept(this);

    return binary(
        ctx.bitExpr(0).accept(this),
        right,
        ctx.BITWISE_XOR_OPERATOR() != null
            ? BinaryOp.BITWISE_XOR
            : BinaryOp.ofOp(ctx.op.getText()));
  }

  @Override
  public ASTNode visitWindowDefinition(MySQLParser.WindowDefinitionContext ctx) {
    final ASTNode node = ctx.windowSpec().accept(this);
    if (ctx.windowName() != null)
      node.set(WINDOW_SPEC_ALIAS, stringifyIdentifier(ctx.windowName().identifier()));
    return node;
  }

  @Override
  public ASTNode visitWindowSpecDetails(MySQLParser.WindowSpecDetailsContext ctx) {
    final ASTNode node = newNode(WINDOW_SPEC);
    if (ctx.windowName() != null)
      node.set(WINDOW_SPEC_NAME, stringifyIdentifier(ctx.windowName().identifier()));
    // partition by
    // in .g4 partition by clause is defined as an orderList
    // but it should be just a expression list
    if (ctx.orderList() != null)
      node.set(
          WINDOW_SPEC_PARTITION,
          ctx.orderList().orderExpression().stream()
              .map(MySQLParser.OrderExpressionContext::expr)
              .map(it -> it.accept(this))
              .collect(Collectors.toList()));
    // order by
    if (ctx.orderClause() != null) node.set(WINDOW_SPEC_ORDER, toOrderItems(ctx.orderClause()));
    if (ctx.windowFrameClause() != null)
      node.set(WINDOW_SPEC_FRAME, visitWindowFrameClause(ctx.windowFrameClause()));

    return node;
  }

  @Override
  public ASTNode visitWindowFrameClause(MySQLParser.WindowFrameClauseContext ctx) {
    final ASTNode node = newNode(WINDOW_FRAME);

    node.set(WINDOW_FRAME_UNIT, WindowUnit.valueOf(ctx.windowFrameUnits().getText().toUpperCase()));

    final var frameExclusion = ctx.windowFrameExclusion();
    if (frameExclusion != null) {
      final WindowExclusion exclusion;
      if (frameExclusion.CURRENT_SYMBOL() != null) exclusion = WindowExclusion.CURRENT_ROW;
      else if (frameExclusion.GROUP_SYMBOL() != null) exclusion = WindowExclusion.GROUP;
      else if (frameExclusion.TIES_SYMBOL() != null) exclusion = WindowExclusion.TIES;
      else if (frameExclusion.OTHERS_SYMBOL() != null) exclusion = WindowExclusion.NO_OTHERS;
      else return assertFalse();
      node.set(WINDOW_FRAME_EXCLUSION, exclusion);
    }

    final var frameExtent = ctx.windowFrameExtent();
    if (frameExtent.windowFrameStart() != null) {
      node.set(WINDOW_FRAME_START, visitWindowFrameStart(frameExtent.windowFrameStart()));

    } else if (frameExtent.windowFrameBetween() != null) {
      final var frameBetween = frameExtent.windowFrameBetween();
      node.set(WINDOW_FRAME_START, visitWindowFrameBound(frameBetween.windowFrameBound(0)));
      node.set(WINDOW_FRAME_END, visitWindowFrameBound(frameBetween.windowFrameBound(1)));

    } else return assertFalse();

    return node;
  }

  @Override
  public ASTNode visitWindowFrameStart(MySQLParser.WindowFrameStartContext ctx) {
    final ASTNode node = newNode(FRAME_BOUND);
    if (ctx.PRECEDING_SYMBOL() != null) {
      node.set(FRAME_BOUND_DIRECTION, FrameBoundDirection.PRECEDING);

      if (ctx.UNBOUNDED_SYMBOL() != null) node.set(FRAME_BOUND_EXPR, symbol("unbounded"));
      else if (ctx.PARAM_MARKER() != null) node.set(FRAME_BOUND_EXPR, paramMarker());
      else if (ctx.ulonglong_number() != null)
        node.set(FRAME_BOUND_EXPR, visitUlonglong_number(ctx.ulonglong_number()));
      else if (ctx.INTERVAL_SYMBOL() != null) {
        final ASTNode interval = newNode(INTERVAL);
        interval.set(INTERVAL_EXPR, toExpr(ctx.expr()));
        interval.set(INTERVAL_UNIT, parseIntervalUnit(ctx.interval()));
        node.set(FRAME_BOUND_EXPR, interval);
      }

    } else node.set(FRAME_BOUND_EXPR, symbol("current row"));

    return node;
  }

  @Override
  public ASTNode visitWindowFrameBound(MySQLParser.WindowFrameBoundContext ctx) {
    if (ctx.windowFrameStart() != null) return visitWindowFrameStart(ctx.windowFrameStart());

    final ASTNode node = newNode(FRAME_BOUND);
    node.set(FRAME_BOUND_DIRECTION, FrameBoundDirection.FOLLOWING);

    if (ctx.UNBOUNDED_SYMBOL() != null) node.set(FRAME_BOUND_EXPR, symbol("unbounded"));
    else if (ctx.PARAM_MARKER() != null) node.set(FRAME_BOUND_EXPR, paramMarker());
    else if (ctx.ulonglong_number() != null)
      node.set(FRAME_BOUND_EXPR, visitUlonglong_number(ctx.ulonglong_number()));
    else if (ctx.INTERVAL_SYMBOL() != null) {
      final ASTNode interval = newNode(INTERVAL);
      interval.set(INTERVAL_EXPR, toExpr(ctx.expr()));
      interval.set(INTERVAL_UNIT, parseIntervalUnit(ctx.interval()));
    }

    return node;
  }

  @Override
  public ASTNode visitUlonglong_number(MySQLParser.Ulonglong_numberContext ctx) {
    final ASTNode node = newNode(LITERAL);

    final Token token =
        Commons.coalesce(
                ctx.INT_NUMBER(),
                ctx.LONG_NUMBER(),
                ctx.ULONGLONG_NUMBER(),
                ctx.DECIMAL_NUMBER(),
                ctx.FLOAT_NUMBER())
            .getSymbol();

    final Pair<LiteralType, Number> pair = parseNumericLiteral(token);
    assert pair != null;

    node.set(LITERAL_TYPE, pair.getLeft());
    node.set(LITERAL_VALUE, pair.getRight());

    return node;
  }

  @Override
  public ASTNode visitSimpleExprColumnRef(MySQLParser.SimpleExprColumnRefContext ctx) {
    final ASTNode columnRef = visitColumnRef(ctx.columnRef());
    if (ctx.jsonOperator() == null) {
      return columnRef;
    } else {
      final ASTNode node = newNode(FUNC_CALL);
      final var jsonOperator = ctx.jsonOperator();

      node.set(FUNC_CALL_NAME, name2(null, "json_extract"));
      node.set(
          FUNC_CALL_ARGS,
          Arrays.asList(
              columnRef,
              literal(LiteralType.TEXT, stringifyText(jsonOperator.textStringLiteral()))));

      if (jsonOperator.JSON_UNQUOTED_SEPARATOR_SYMBOL() == null) return node;
      else {
        final ASTNode unquoteCall = newNode(FUNC_CALL);

        unquoteCall.set(FUNC_CALL_NAME, name2(null, "json_unquote"));
        unquoteCall.set(FUNC_CALL_ARGS, singletonList(node));

        return unquoteCall;
      }
    }
  }

  @Override
  public ASTNode visitColumnRef(MySQLParser.ColumnRefContext ctx) {
    final ASTNode node = newNode(COLUMN_REF);

    final ASTNode column = newNode(COLUMN_NAME);
    final String[] triple = stringifyIdentifier(ctx.fieldIdentifier());
    column.set(COLUMN_NAME_SCHEMA, triple[0]);
    column.set(COLUMN_NAME_TABLE, triple[1]);
    column.set(COLUMN_NAME_COLUMN, triple[2]);

    node.set(COLUMN_REF_COLUMN, column);

    return node;
  }

  @Override
  public ASTNode visitVariable(MySQLParser.VariableContext ctx) {
    final ASTNode node = newNode(VARIABLE);
    if (ctx.userVariable() != null) {
      final var userVariable = ctx.userVariable();
      final String name;
      if (userVariable.textOrIdentifier() != null)
        name = stringifyText(userVariable.textOrIdentifier());
      else if (userVariable.AT_TEXT_SUFFIX() != null)
        name = userVariable.AT_SIGN_SYMBOL().getText().substring(1);
      else return assertFalse();

      node.set(VARIABLE_NAME, name);
      node.set(VARIABLE_SCOPE, VariableScope.USER);
      if (ctx.expr() != null) node.set(VARIABLE_ASSIGNMENT, toExpr(ctx.expr()));

    } else if (ctx.systemVariable() != null) {
      final var systemVariable = ctx.systemVariable();
      final var varIdentType = systemVariable.varIdentType();

      final VariableScope scope;
      if (varIdentType == null) scope = VariableScope.SYSTEM_GLOBAL;
      else {
        final String text = varIdentType.getText();
        final String scopeName = "SYSTEM_" + text.substring(0, text.length() - 1).toUpperCase();
        scope = VariableScope.valueOf(scopeName);
      }

      final StringBuilder builder = new StringBuilder();
      builder.append(stringifyText(systemVariable.textOrIdentifier()));
      if (systemVariable.dotIdentifier() != null)
        builder.append('.').append(stringifyIdentifier(systemVariable.dotIdentifier()));
      final String name = builder.toString().toLowerCase();

      node.set(VARIABLE_SCOPE, scope);
      node.set(VARIABLE_NAME, name);

    } else return assertFalse();

    return node;
  }

  @Override
  public ASTNode visitRuntimeFunctionCall(MySQLParser.RuntimeFunctionCallContext ctx) {
    final ASTNode node = newNode(FUNC_CALL);

    final List<ASTNode> args;

    if (ctx.name != null) {
      node.set(FUNC_CALL_NAME, name2(null, ctx.name.getText().toLowerCase()));

      if (ctx.parentheses() != null) {
        // no-arg
        args = emptyList();

      } else if (ctx.exprWithParentheses() != null) {
        // single-arg
        args = singletonList(toExpr(ctx.exprWithParentheses().expr()));

      } else if (ctx.exprListWithParentheses() != null) {
        // var-arg
        args = toExprs(ctx.exprListWithParentheses().exprList());

      } else if (ctx.exprList() != null) {
        args = toExprs(ctx.exprList());

      } else if (ctx.INTERVAL_SYMBOL() != null) {
        // ADDDATE/SUBDATE/DATE_ADD/DATE_SUB with INTERVAL expr
        final ASTNode interval = newNode(INTERVAL);
        interval.set(INTERVAL_EXPR, toExpr(ctx.expr(1)));
        interval.set(INTERVAL_UNIT, parseIntervalUnit(ctx.interval()));
        args = Arrays.asList(toExpr(ctx.expr(0)), interval);

      } else if (ctx.interval() != null) {
        // EXTRACT
        args = Arrays.asList(symbol(ctx.interval().getText().toLowerCase()), toExpr(ctx.expr(0)));

      } else if (ctx.dateTimeTtype() != null) {
        // GET_FORMAT
        args =
            Arrays.asList(
                symbol(ctx.dateTimeTtype().getText().toLowerCase()), ctx.expr(0).accept(this));

      } else if (ctx.bitExpr() != null) {
        // POSITION
        args = Arrays.asList(visit(ctx.bitExpr()), toExpr(ctx.expr(0)));

      } else if (ctx.intervalTimeStamp() != null) {
        // TIMESTAMP_ADD/TIMESTAMP_DIFF
        args =
            Arrays.asList(
                symbol(ctx.intervalTimeStamp().getText().toLowerCase()),
                toExpr(ctx.expr(0)),
                toExpr(ctx.expr(1)));

      } else if (ctx.textLiteral() != null) {
        // OLD_PASSWORD
        args = singletonList(literal(LiteralType.TEXT, stringifyText(ctx.textLiteral())));

      } else if (ctx.timeFunctionParameters() != null) {
        final var fractionalPrecision = ctx.timeFunctionParameters().fractionalPrecision();
        if (fractionalPrecision != null)
          args =
              singletonList(
                  literal(LiteralType.INTEGER, Integer.parseInt(fractionalPrecision.getText())));
        else args = emptyList();

      } else if (ctx.expr() != null) {
        args = listMap(ctx.expr(), this::toExpr);

      } else {
        args = emptyList();
      }
    } else if (ctx.trimFunction() != null) {
      node.set(FUNC_CALL_NAME, name2(null, "trim"));
      args = new ArrayList<>(3);

      final var trimFunc = ctx.trimFunction();
      if (trimFunc.LEADING_SYMBOL() != null) args.add(symbol("leading"));
      else if (trimFunc.TRAILING_SYMBOL() != null) args.add(symbol("trailing"));
      else if (trimFunc.BOTH_SYMBOL() != null) args.add(symbol("both"));
      else args.add(null);

      final var exprs = trimFunc.expr();
      if (args.get(0) == null) {
        args.add(toExpr(exprs.get(0)));
        if (exprs.size() == 2) args.add(toExpr(exprs.get(1)));
        else args.add(null);
      } else {
        if (exprs.size() == 2) {
          args.add(toExpr(exprs.get(0)));
          args.add(toExpr(exprs.get(1)));
        } else {
          args.add(null);
          args.add(toExpr(exprs.get(0)));
        }
      }

    } else if (ctx.substringFunction() != null) {
      node.set(FUNC_CALL_NAME, name2(null, "substring"));
      final var substringFunc = ctx.substringFunction();
      args = listMap(substringFunc.expr(), this::toExpr);

    } else if (ctx.geometryFunction() != null) {
      final var geoFunc = ctx.geometryFunction();
      node.set(FUNC_CALL_NAME, name2(null, geoFunc.name.getText().toLowerCase()));

      if (geoFunc.exprListWithParentheses() != null) {
        // var-arg
        args = toExprs(geoFunc.exprListWithParentheses().exprList());
      } else if (geoFunc.exprList() != null) {
        args = toExprs(geoFunc.exprList());

      } else if (geoFunc.expr() != null) {
        args = listMap(geoFunc.expr(), this::toExpr);

      } else {
        args = emptyList();
      }

    } else {
      args = emptyList();
    }

    node.set(FUNC_CALL_ARGS, args);

    return node;
  }

  @Override
  public ASTNode visitSimpleExprCollate(MySQLParser.SimpleExprCollateContext ctx) {
    final ASTNode node = newNode(COLLATE);

    node.set(COLLATE_EXPR, ctx.simpleExpr().accept(this));
    node.set(COLLATE_COLLATION, symbol(stringifyText(ctx.textOrIdentifier())));

    return node;
  }

  @Override
  public ASTNode visitFunctionCall(MySQLParser.FunctionCallContext ctx) {
    final ASTNode node = newNode(FUNC_CALL);

    if (ctx.pureIdentifier() != null) {
      node.set(
          FUNC_CALL_NAME, name2(null, stringifyIdentifier(ctx.pureIdentifier()).toLowerCase()));

      if (ctx.udfExprList() != null) node.set(FUNC_CALL_ARGS, toExprs(ctx.udfExprList()));
      else node.set(FUNC_CALL_ARGS, emptyList());

    } else if (ctx.qualifiedIdentifier() != null) {
      node.set(
          FUNC_CALL_NAME,
          name2(null, stringifyIdentifier(ctx.qualifiedIdentifier())[1].toLowerCase()));

      if (ctx.exprList() != null) node.set(FUNC_CALL_ARGS, toExprs((ctx.exprList())));
      else node.set(FUNC_CALL_ARGS, emptyList());

    } else {
      return assertFalse();
    }

    return node;
  }

  @Override
  public ASTNode visitLiteral(MySQLParser.LiteralContext ctx) {
    final ASTNode node = newNode(LITERAL);

    final LiteralType type;
    final Object value;
    String unit = null;

    if (ctx.textLiteral() != null) {
      type = LiteralType.TEXT;
      value = stringifyText(ctx.textLiteral());

    } else if (ctx.numLiteral() != null) {
      final var num = ctx.numLiteral();
      if (num.INT_NUMBER() != null) {
        type = LiteralType.INTEGER;
        value = Integer.parseInt(num.INT_NUMBER().getText());

      } else if (num.LONG_NUMBER() != null || num.ULONGLONG_NUMBER() != null) {
        type = LiteralType.LONG;
        value = Long.parseLong(num.getText());
      } else if (num.FLOAT_NUMBER() != null || num.DECIMAL_NUMBER() != null) {
        type = LiteralType.FRACTIONAL;
        value = Double.parseDouble(num.getText());
      } else {
        return assertFalse();
      }
    } else if (ctx.temporalLiteral() != null) {
      final var temporal = ctx.temporalLiteral();
      type = LiteralType.TEMPORAL;
      value = unquoted(temporal.SINGLE_QUOTED_TEXT().getText(), '\'');
      unit =
          temporal.DATE_SYMBOL() != null
              ? "date"
              : temporal.TIME_SYMBOL() != null
                  ? "time"
                  : temporal.TIMESTAMP_SYMBOL() != null ? "timestamp" : null;

    } else if (ctx.nullLiteral() != null) {
      type = LiteralType.NULL;
      value = null;

    } else if (ctx.boolLiteral() != null) {
      type = LiteralType.BOOL;
      value = "true".equalsIgnoreCase(ctx.boolLiteral().getText());
    } else {
      type = LiteralType.HEX;
      value = ctx.HEX_NUMBER() == null ? ctx.HEX_NUMBER().getText() : ctx.BIN_NUMBER().getText();
    }

    node.set(LITERAL_TYPE, type);
    node.set(LITERAL_VALUE, value);
    node.set(LITERAL_UNIT, unit);

    return node;
  }

  @Override
  public ASTNode visitSimpleExprParamMarker(MySQLParser.SimpleExprParamMarkerContext ctx) {
    return paramMarker();
  }

  @Override
  public ASTNode visitSumExpr(MySQLParser.SumExprContext ctx) {
    final ASTNode node = newNode(AGGREGATE);

    node.set(AGGREGATE_NAME, ctx.name.getText().toLowerCase());

    if (ctx.DISTINCT_SYMBOL() != null) node.flag(AGGREGATE_DISTINCT);

    final List<ASTNode> args;
    if (ctx.inSumExpr() != null) args = singletonList(visitInSumExpr(ctx.inSumExpr()));
    else if (ctx.exprList() != null) args = toExprs(ctx.exprList());
    else if (ctx.MULT_OPERATOR() != null) args = singletonList(wildcard());
    else args = emptyList();

    node.set(AGGREGATE_ARGS, args);

    if (ctx.windowingClause() != null) {
      final var windowingClause = ctx.windowingClause();
      if (windowingClause.windowName() != null) {
        node.set(
            AGGREGATE_WINDOW_NAME, stringifyIdentifier(windowingClause.windowName().identifier()));

      } else if (windowingClause.windowSpec() != null) {
        node.set(
            AGGREGATE_WINDOW_SPEC,
            visitWindowSpecDetails(windowingClause.windowSpec().windowSpecDetails()));

      } else return assertFalse();
    }
    if (ctx.orderClause() != null) node.set(AGGREGATE_ORDER, toOrderItems(ctx.orderClause()));
    if (ctx.textString() != null) node.set(AGGREGATE_SEP, stringifyText(ctx.textString()));

    return node;
  }

  @Override
  public ASTNode visitGroupingOperation(MySQLParser.GroupingOperationContext ctx) {
    final ASTNode node = newNode(GROUPING_OP);
    node.set(GROUPING_OP_EXPRS, toExprs(ctx.exprList()));
    return node;
  }

  @Override
  public ASTNode visitWindowFunctionCall(MySQLParser.WindowFunctionCallContext ctx) {
    // TODO
    return super.visitWindowFunctionCall(ctx);
  }

  @Override
  public ASTNode visitSimpleExprConcat(MySQLParser.SimpleExprConcatContext ctx) {
    final ASTNode node = newNode(FUNC_CALL);
    node.set(FUNC_CALL_NAME, name2(null, "concat"));
    node.set(FUNC_CALL_ARGS, listMap(ctx.simpleExpr(), arg -> arg.accept(this)));
    return node;
  }

  @Override
  public ASTNode visitSimpleExprUnary(MySQLParser.SimpleExprUnaryContext ctx) {
    final ASTNode node = newNode(UNARY);
    node.set(UNARY_OP, UnaryOp.ofOp(ctx.op.getText()));
    node.set(UNARY_EXPR, ctx.simpleExpr().accept(this));
    return node;
  }

  @Override
  public ASTNode visitSimpleExprNot(MySQLParser.SimpleExprNotContext ctx) {
    final ASTNode node = newNode(UNARY);
    node.set(UNARY_OP, UnaryOp.NOT);
    node.set(UNARY_EXPR, ctx.simpleExpr().accept(this));
    return node;
  }

  @Override
  public ASTNode visitSimpleExprList(MySQLParser.SimpleExprListContext ctx) {
    final List<ASTNode> exprs = toExprs(ctx.exprList());
    final boolean asRow = ctx.ROW_SYMBOL() != null;
    if (!asRow && exprs.size() == 1) return exprs.get(0);

    final ASTNode node = newNode(TUPLE);
    node.set(TUPLE_EXPRS, exprs);
    if (asRow) node.flag(TUPLE_AS_ROW);

    return node;
  }

  @Override
  public ASTNode visitSimpleExprSubQuery(MySQLParser.SimpleExprSubQueryContext ctx) {
    final ASTNode subquery = wrapAsQueryExpr(this, visitSubquery(ctx.subquery()));

    if (ctx.EXISTS_SYMBOL() == null) return subquery;

    final ASTNode node = newNode(EXISTS);
    node.set(EXISTS_SUBQUERY_EXPR, subquery);
    return node;
  }

  @Override
  public ASTNode visitSimpleExprMatch(MySQLParser.SimpleExprMatchContext ctx) {
    final ASTNode node = newNode(MATCH);
    node.set(
        MATCH_COLS,
        listMap(
            ctx.identListArg().identList().simpleIdentifier(),
            func(this::visitSimpleIdentifier).andThen(this::columnRef)));
    node.set(MATCH_EXPR, ctx.bitExpr().accept(this));

    final var fullTextOption = ctx.fulltextOptions();
    if (fullTextOption != null) {
      final MatchOption option;
      if (fullTextOption.BOOLEAN_SYMBOL() != null) {
        option = MatchOption.BOOLEAN_MODE;
      } else if (fullTextOption.EXPANSION_SYMBOL() != null
          && fullTextOption.NATURAL_SYMBOL() != null) {
        option = MatchOption.NATURAL_MODE_WITH_EXPANSION;
      } else if (fullTextOption.EXPANSION_SYMBOL() != null) {
        option = MatchOption.WITH_EXPANSION;
      } else {
        option = null;
      }
      node.set(MATCH_OPTION, option);
    }

    return node;
  }

  @Override
  public ASTNode visitSimpleExprBinary(MySQLParser.SimpleExprBinaryContext ctx) {
    final ASTNode node = newNode(UNARY);
    node.set(UNARY_OP, UnaryOp.BINARY);
    node.set(UNARY_EXPR, ctx.simpleExpr().accept(this));
    return node;
  }

  @Override
  public ASTNode visitSimpleExprCast(MySQLParser.SimpleExprCastContext ctx) {
    final ASTNode node = newNode(CAST);
    node.set(CAST_EXPR, toExpr(ctx.expr()));
    node.set(CAST_TYPE, parseDataType(ctx.castType()));
    node.set(CAST_IS_ARRAY, ctx.arrayCast() != null);
    return node;
  }

  @Override
  public ASTNode visitSimpleExprConvert(MySQLParser.SimpleExprConvertContext ctx) {
    final ASTNode node = newNode(CAST);
    node.set(CAST_EXPR, toExpr(ctx.expr()));
    node.set(CAST_TYPE, parseDataType(ctx.castType()));
    return node;
  }

  @Override
  public ASTNode visitSimpleExprCase(MySQLParser.SimpleExprCaseContext ctx) {
    final ASTNode node = newNode(CASE);
    if (ctx.expr() != null) node.set(CASE_COND, toExpr(ctx.expr()));
    if (ctx.elseExpression() != null) node.set(CASE_ELSE, toExpr(ctx.elseExpression().expr()));
    if (ctx.whenExpression() != null) {
      final var whenExprs = ctx.whenExpression();
      final var thenExprs = ctx.thenExpression();
      final List<ASTNode> whens = new ArrayList<>(whenExprs.size());

      for (int i = 0; i < whenExprs.size(); i++) {
        final var whenExpr = whenExprs.get(i);
        final var thenExpr = thenExprs.get(i);

        final ASTNode when = newNode(WHEN);
        when.set(WHEN_COND, toExpr(whenExpr.expr()));
        when.set(WHEN_EXPR, toExpr(thenExpr.expr()));
        whens.add(when);
      }

      node.set(CASE_WHENS, whens);
    }

    return node;
  }

  @Override
  public ASTNode visitSimpleExprConvertUsing(MySQLParser.SimpleExprConvertUsingContext ctx) {
    final ASTNode node = newNode(CONVERT_USING);
    node.set(CONVERT_USING_EXPR, toExpr(ctx.expr()));
    node.set(CONVERT_USING_CHARSET, visitCharsetName(ctx.charsetName()));
    return node;
  }

  @Override
  public ASTNode visitSimpleExprDefault(MySQLParser.SimpleExprDefaultContext ctx) {
    final ASTNode node = newNode(DEFAULT);
    node.set(DEFAULT_COL, visitSimpleIdentifier(ctx.simpleIdentifier()));
    return node;
  }

  @Override
  public ASTNode visitSimpleExprValues(MySQLParser.SimpleExprValuesContext ctx) {
    final ASTNode node = newNode(VALUES);
    node.set(VALUES_EXPR, visitSimpleIdentifier(ctx.simpleIdentifier()));
    return node;
  }

  @Override
  public ASTNode visitSimpleExprInterval(MySQLParser.SimpleExprIntervalContext ctx) {
    final ASTNode interval = newNode(INTERVAL);
    interval.set(INTERVAL_EXPR, toExpr(ctx.expr(0)));
    interval.set(INTERVAL_UNIT, parseIntervalUnit(ctx.interval()));

    return binary(interval, toExpr(ctx.expr(1)), BinaryOp.PLUS);
  }

  @Override
  public ASTNode visitSimpleIdentifier(MySQLParser.SimpleIdentifierContext ctx) {
    final ASTNode node = newNode(COLUMN_NAME);
    final String[] triple = stringifyIdentifier(ctx);
    assert triple != null;
    node.set(COLUMN_NAME_SCHEMA, triple[0]);
    node.set(COLUMN_NAME_TABLE, triple[1]);
    node.set(COLUMN_NAME_COLUMN, triple[2]);
    return node;
  }

  @Override
  public ASTNode visitInSumExpr(MySQLParser.InSumExprContext ctx) {
    return toExpr(ctx.expr());
  }

  @Override
  public ASTNode visitCharsetName(MySQLParser.CharsetNameContext ctx) {
    final String name;
    if (ctx.textOrIdentifier() != null) name = stringifyText(ctx.textOrIdentifier());
    else if (ctx.BINARY_SYMBOL() != null) name = "binary";
    else if (ctx.DEFAULT_SYMBOL() != null) name = "default";
    else return assertFalse();
    return symbol(name.toLowerCase());
  }

  @Override
  public ASTNode visitOrderExpression(MySQLParser.OrderExpressionContext ctx) {
    final ASTNode node = newNode(ORDER_ITEM);

    final ASTNode expr = ctx.expr().accept(this);
    final KeyDirection direction = parseDirection(ctx.direction());
    node.set(ORDER_ITEM_EXPR, expr);
    node.set(ORDER_ITEM_DIRECTION, direction);

    return node;
  }

  private void parseFieldDefinition(MySQLParser.FieldDefinitionContext fieldDef, ASTNode node) {
    node.set(COLUMN_DEF_DATATYPE_RAW, fieldDef.dataType().getText().toLowerCase());
    node.set(COLUMN_DEF_DATATYPE, parseDataType(fieldDef.dataType()));

    collectColumnAttrs(fieldDef.columnAttribute(), node);
    collectGColumnAttrs(fieldDef.gcolAttribute(), node);

    if (fieldDef.AS_SYMBOL() != null) node.flag(COLUMN_DEF_GENERATED);
  }

  private List<ASTNode> toOrderItems(MySQLParser.OrderClauseContext ctx) {
    return listMap(ctx.orderList().orderExpression(), this::visitOrderExpression);
  }

  private List<ASTNode> toGroupItems(MySQLParser.OrderListContext ctx) {
    return ctx.orderExpression().stream()
        .map(MySQLParser.OrderExpressionContext::expr)
        .map(it -> it.accept(this))
        .map(this::groupItem)
        .collect(Collectors.toList());
  }

  private ASTNode toExpr(MySQLParser.ExprContext expr) {
    return expr.accept(this);
  }

  private List<ASTNode> toExprs(MySQLParser.ExprListContext exprList) {
    return listMap(exprList.expr(), this::toExpr);
  }

  private List<ASTNode> toExprs(MySQLParser.UdfExprListContext udfExprList) {
    return listMap(udfExprList.udfExpr(), udfExpr -> toExpr(udfExpr.expr()));
  }
}
