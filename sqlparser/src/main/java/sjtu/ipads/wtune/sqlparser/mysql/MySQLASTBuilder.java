package sjtu.ipads.wtune.sqlparser.mysql;

import sjtu.ipads.wtune.common.utils.FuncUtils;
import sjtu.ipads.wtune.sqlparser.SQLNode;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.FuncUtils.coalesce;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.ConstraintType.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.IndexType.FULLTEXT;
import static sjtu.ipads.wtune.sqlparser.SQLNode.IndexType.SPATIAL;
import static sjtu.ipads.wtune.sqlparser.mysql.MySQLASTHelper.*;

public class MySQLASTBuilder extends MySQLParserBaseVisitor<SQLNode> {
  @Override
  protected SQLNode aggregateResult(SQLNode aggregate, SQLNode nextResult) {
    return coalesce(aggregate, nextResult);
  }

  @Override
  public SQLNode visitTableName(MySQLParser.TableNameContext tableName) {
    return tableName(tableName.qualifiedIdentifier(), tableName.dotIdentifier());
  }

  @Override
  public SQLNode visitTableRef(MySQLParser.TableRefContext tableName) {
    return tableName(tableName.qualifiedIdentifier(), tableName.dotIdentifier());
  }

  @Override
  public SQLNode visitColumnName(MySQLParser.ColumnNameContext ctx) {
    final var node = newNode(SQLNode.Type.COLUMN_NAME);
    final String schema, table, column;

    if (ctx.identifier() != null) {
      schema = null;
      table = null;
      column = stringifyIdentifier(ctx.identifier());

    } else if (ctx.fieldIdentifier() != null) {
      final var triple = stringifyIdentifier(ctx.fieldIdentifier());
      schema = triple[0];
      table = triple[1];
      column = triple[2];

    } else {
      assert false;
      return null;
    }

    node.put(COLUMN_NAME_SCHEMA, schema);
    node.put(COLUMN_NAME_TABLE, table);
    node.put(COLUMN_NAME_COLUMN, column);

    return node;
  }

  @Override
  public SQLNode visitCreateTable(MySQLParser.CreateTableContext ctx) {
    final var node = newNode(SQLNode.Type.CREATE_TABLE);
    node.put(CREATE_TABLE_NAME, visitTableName(ctx.tableName()));

    if (ctx.tableElementList() != null) {
      final List<SQLNode> columnDefs = new ArrayList<>();
      final List<SQLNode> contraintDefs = new ArrayList<>();

      for (var element : ctx.tableElementList().tableElement()) {
        final var colDefs = element.columnDefinition();
        final var consDefs = element.tableConstraintDef();

        if (colDefs != null) columnDefs.add(visitColumnDefinition(colDefs));
        else if (consDefs != null) contraintDefs.add(visitTableConstraintDef(consDefs));
        else assert false;
      }

      node.put(CREATE_TABLE_COLUMNS, columnDefs);
      node.put(CREATE_TABLE_CONSTRAINTS, contraintDefs);

      final var tableOptions = ctx.createTableOptions();
      if (tableOptions != null)
        for (var option : tableOptions.createTableOption())
          if (option.ENGINE_SYMBOL() != null) {
            node.put(CREATE_TABLE_ENGINE, stringifyText(option.engineRef().textOrIdentifier()));
            break;
          }

    } else if (ctx.tableRef() != null) {

    }

    return node.relinkAll();
  }

  @Override
  public SQLNode visitColumnDefinition(MySQLParser.ColumnDefinitionContext ctx) {
    final var node = newNode(Type.COLUMN_DEF);
    node.put(COLUMN_DEF_NAME, visitColumnName(ctx.columnName()));

    final var fieldDef = ctx.fieldDefinition();
    node.put(COLUMN_DEF_DATATYPE_RAW, fieldDef.dataType().getText());
    node.put(COLUMN_DEF_DATATYPE, parseDataType(fieldDef.dataType()));

    collectColumnAttrs(fieldDef.columnAttribute(), node);
    collectGColumnAttrs(fieldDef.gcolAttribute(), node);

    if (fieldDef.AS_SYMBOL() != null) node.flag(COLUMN_DEF_GENERATED);

    final var checkOrRef = ctx.checkOrReferences();
    if (checkOrRef != null)
      if (checkOrRef.references() != null)
        node.put(COLUMN_DEF_REF, visitReferences(checkOrRef.references()));
      else if (checkOrRef.checkConstraint() != null) node.flag(COLUMN_DEF_CONS, CHECK);

    return node;
  }

  @Override
  public SQLNode visitReferences(MySQLParser.ReferencesContext ctx) {
    final var node = newNode(Type.REFERENCES);

    node.put(REFERENCES_TABLE, visitTableRef(ctx.tableRef()));

    final var idList = ctx.identifierListWithParentheses();
    if (idList != null) {
      final var ids = idList.identifierList().identifier();
      final List<SQLNode> columns = new ArrayList<>(ids.size());

      for (var id : ids) {
        final var columnRef = newNode(Type.COLUMN_NAME);
        columnRef.put(COLUMN_NAME_COLUMN, stringifyIdentifier(id));
        columns.add(columnRef);
      }
      node.put(REFERENCES_COLUMNS, columns);
    }

    return node;
  }

  @Override
  public SQLNode visitTableConstraintDef(MySQLParser.TableConstraintDefContext ctx) {
    final var node = newNode(Type.INDEX_DEF);
    final var type = ctx.type;
    if (type == null) return null;

    final var indexNameAndType = ctx.indexNameAndType();
    final var indexName = ctx.indexName();
    final var indexOptions = ctx.indexOption();

    final ConstraintType c;
    final IndexType t;
    final String name;
    switch (type.getText().toLowerCase()) {
      case "index":
        c = null;
        t = FuncUtils.coalesce(parseIndexType(indexNameAndType), parseIndexType(indexOptions));
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
        t = FuncUtils.coalesce(parseIndexType(indexNameAndType), parseIndexType(indexOptions));
        name = stringifyIndexName(indexNameAndType);
        break;

      case "unique":
        c = UNIQUE;
        t = FuncUtils.coalesce(parseIndexType(indexNameAndType), parseIndexType(indexOptions));
        name = stringifyIndexName(indexNameAndType);
        break;

      case "foreign":
        c = FOREIGN;
        t = null;
        name = stringifyIndexName(indexName);
        break;

      default:
        assert false;
        return null;
    }

    node.put(INDEX_DEF_CONS, c);
    node.put(INDEX_DEF_TYPE, t);
    node.put(INDEX_DEF_NAME, name);

    final var keyListVariants = ctx.keyListVariants();
    final var keyList = keyListVariants != null ? keyListVariants.keyList() : ctx.keyList();
    final List<SQLNode> keys;

    if (keyList != null) {
      keys = FuncUtils.listMap(this::visitKeyPart, keyList.keyPart());
    } else if (keyListVariants != null) {
      keys =
          FuncUtils.listMap(
              this::visitKeyPartOrExpression,
              keyListVariants.keyListWithExpression().keyPartOrExpression());
    } else {
      assert false;
      return null;
    }

    node.put(INDEX_DEF_KEYS, keys);

    final var references = ctx.references();
    if (references != null) node.put(INDEX_DEF_REFS, visitReferences(references));

    return node;
  }

  @Override
  public SQLNode visitKeyPart(MySQLParser.KeyPartContext ctx) {
    final var node = newNode(Type.KEY_PART);
    node.put(KEY_PART_COLUMN, stringifyIdentifier(ctx.identifier()));
    node.put(KEY_PART_DIRECTION, parseDirection(ctx.direction()));
    if (ctx.fieldLength() != null) node.put(KEY_PART_LEN, fieldLength2Int(ctx.fieldLength()));
    return node;
  }

  @Override
  public SQLNode visitKeyPartOrExpression(MySQLParser.KeyPartOrExpressionContext ctx) {
    if (ctx.keyPart() != null) return visitKeyPart(ctx.keyPart());
    else if (ctx.exprWithParentheses() != null) return null; // TODO
    else return null;
  }

  @Override
  public SQLNode visitCreateIndex(MySQLParser.CreateIndexContext ctx) {
    return super.visitCreateIndex(ctx);
  }

  @Override
  public SQLNode visitSelectStatement(MySQLParser.SelectStatementContext ctx) {
    return super.visitSelectStatement(ctx);
  }

  @Override
  public SQLNode visitInsertStatement(MySQLParser.InsertStatementContext ctx) {
    return super.visitInsertStatement(ctx);
  }

  @Override
  public SQLNode visitDeleteStatement(MySQLParser.DeleteStatementContext ctx) {
    return super.visitDeleteStatement(ctx);
  }

  @Override
  public SQLNode visitUpdateStatement(MySQLParser.UpdateStatementContext ctx) {
    return super.visitUpdateStatement(ctx);
  }
}
