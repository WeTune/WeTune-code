package sjtu.ipads.wtune.sqlparser.pg;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.pg.internal.PGParser;
import sjtu.ipads.wtune.sqlparser.pg.internal.PGParserBaseVisitor;

import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.Type.COLUMN_DEF;
import static sjtu.ipads.wtune.sqlparser.pg.PGASTHelper.*;

public class PGASTBuilder extends PGParserBaseVisitor<SQLNode> {
  @Override
  public SQLNode visitSchema_qualified_name(PGParser.Schema_qualified_nameContext ctx) {
    return tableName(ctx);
  }

  @Override
  public SQLNode visitCreate_table_statement(PGParser.Create_table_statementContext ctx) {
    return null;
  }

  @Override
  public SQLNode visitTable_column_definition(PGParser.Table_column_definitionContext ctx) {
    final SQLNode node = new SQLNode(COLUMN_DEF);
    node.put(COLUMN_DEF_NAME, columnName(null, stringifyIdentifier(ctx.identifier())));

    final var dataTypeCtx = ctx.data_type();
    node.put(COLUMN_DEF_DATATYPE_RAW, dataTypeCtx.getText());
    node.put(COLUMN_DEF_DATATYPE, parseDataType(dataTypeCtx));

    return node;
  }

  @Override
  public SQLNode visitConstraint_common(PGParser.Constraint_commonContext ctx) {
    return null;
  }
}
