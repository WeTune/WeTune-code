package sjtu.ipads.wtune.sqlparser.pg;

import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.pg.internal.PGParser;
import sjtu.ipads.wtune.sqlparser.pg.internal.PGParserBaseVisitor;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.exprKind;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.ConstraintType.PRIMARY;
import static sjtu.ipads.wtune.sqlparser.SQLNode.ConstraintType.UNIQUE;
import static sjtu.ipads.wtune.sqlparser.SQLNode.columnName;
import static sjtu.ipads.wtune.sqlparser.SQLNode.Type.*;
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
    if (expr == null) return null; // TODO: remove this

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
}
