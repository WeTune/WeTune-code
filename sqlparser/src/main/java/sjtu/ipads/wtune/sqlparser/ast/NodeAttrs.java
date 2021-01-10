package sjtu.ipads.wtune.sqlparser.ast;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.ast.constants.*;

import java.util.EnumSet;
import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.*;

public interface NodeAttrs {
  String SQL_ATTR_PREFIX = "sql.attr.";

  //// TableName
  Attrs.Key<String> TABLE_NAME_SCHEMA = TABLE_NAME.strAttr("schema");
  Attrs.Key<String> TABLE_NAME_TABLE = TABLE_NAME.strAttr("table");

  //// ColumnName
  Attrs.Key<String> COLUMN_NAME_SCHEMA = COLUMN_NAME.strAttr("schema");
  Attrs.Key<String> COLUMN_NAME_TABLE = COLUMN_NAME.strAttr("table");
  Attrs.Key<String> COLUMN_NAME_COLUMN = COLUMN_NAME.strAttr("column");

  //// CommonName2
  Attrs.Key<String> NAME_2_0 = NAME_2.strAttr("part0");
  Attrs.Key<String> NAME_2_1 = NAME_2.strAttr("part1");

  //// CommonName3
  Attrs.Key<String> NAME_3_0 = NAME_3.strAttr("part0");
  Attrs.Key<String> NAME_3_1 = NAME_3.strAttr("part1");
  Attrs.Key<String> NAME_3_2 = NAME_3.strAttr("part2");

  //// CreateTable
  Attrs.Key<SQLNode> CREATE_TABLE_NAME = CREATE_TABLE.nodeAttr("name");
  Attrs.Key<List<SQLNode>> CREATE_TABLE_COLUMNS = CREATE_TABLE.nodesAttr("columns");
  Attrs.Key<List<SQLNode>> CREATE_TABLE_CONSTRAINTS = CREATE_TABLE.nodesAttr("constraints");
  Attrs.Key<String> CREATE_TABLE_ENGINE = CREATE_TABLE.strAttr("engine");

  //// ColumnDef
  Attrs.Key<SQLNode> COLUMN_DEF_NAME = COLUMN_DEF.nodeAttr("name");
  Attrs.Key<String> COLUMN_DEF_DATATYPE_RAW = COLUMN_DEF.strAttr("typeRaw");
  Attrs.Key<SQLDataType> COLUMN_DEF_DATATYPE = COLUMN_DEF.attr("dataType", SQLDataType.class);
  Attrs.Key<EnumSet<ConstraintType>> COLUMN_DEF_CONS =
      COLUMN_DEF.attr2("constraint", EnumSet.class);
  Attrs.Key<SQLNode> COLUMN_DEF_REF = COLUMN_DEF.nodeAttr("references");
  Attrs.Key<Boolean> COLUMN_DEF_GENERATED = COLUMN_DEF.boolAttr("genearted");
  Attrs.Key<Boolean> COLUMN_DEF_DEFAULT = COLUMN_DEF.boolAttr("default");
  Attrs.Key<Boolean> COLUMN_DEF_AUTOINCREMENT = COLUMN_DEF.boolAttr("autoInc");

  //// References
  Attrs.Key<SQLNode> REFERENCES_TABLE = REFERENCES.nodeAttr("table");
  Attrs.Key<List<SQLNode>> REFERENCES_COLUMNS = REFERENCES.nodesAttr("columns");

  //// IndexDef
  Attrs.Key<String> INDEX_DEF_NAME = INDEX_DEF.strAttr("name");
  Attrs.Key<SQLNode> INDEX_DEF_TABLE = INDEX_DEF.nodeAttr("table");
  Attrs.Key<IndexType> INDEX_DEF_TYPE = INDEX_DEF.attr("type", IndexType.class);
  Attrs.Key<ConstraintType> INDEX_DEF_CONS = INDEX_DEF.attr("constraint", ConstraintType.class);
  Attrs.Key<List<SQLNode>> INDEX_DEF_KEYS = INDEX_DEF.nodesAttr("keys");
  Attrs.Key<SQLNode> INDEX_DEF_REFS = INDEX_DEF.nodeAttr("references");

  //// KeyPart
  Attrs.Key<String> KEY_PART_COLUMN = KEY_PART.strAttr("column");
  Attrs.Key<Integer> KEY_PART_LEN = KEY_PART.attr("length", Integer.class);
  Attrs.Key<SQLNode> KEY_PART_EXPR = KEY_PART.nodeAttr("expr");
  Attrs.Key<KeyDirection> KEY_PART_DIRECTION = KEY_PART.attr("direction", KeyDirection.class);

  //// Union
  Attrs.Key<SQLNode> SET_OP_LEFT = SET_OP.nodeAttr("left");
  Attrs.Key<SQLNode> SET_OP_RIGHT = SET_OP.nodeAttr("right");
  Attrs.Key<SetOperation> SET_OP_TYPE = SET_OP.attr("type", SetOperation.class);
  Attrs.Key<SetOperationOption> SET_OP_OPTION = SET_OP.attr("option", SetOperationOption.class);

  //// Query
  Attrs.Key<SQLNode> QUERY_BODY = QUERY.nodeAttr("body");
  Attrs.Key<List<SQLNode>> QUERY_ORDER_BY = QUERY.nodesAttr("orderBy");
  Attrs.Key<SQLNode> QUERY_LIMIT = QUERY.nodeAttr("limit");
  Attrs.Key<SQLNode> QUERY_OFFSET = QUERY.nodeAttr("offset");

  //// QuerySpec
  Attrs.Key<Boolean> QUERY_SPEC_DISTINCT = QUERY_SPEC.boolAttr("distinct");
  Attrs.Key<List<SQLNode>> QUERY_SPEC_DISTINCT_ON = QUERY_SPEC.nodesAttr("distinctOn");
  Attrs.Key<List<SQLNode>> QUERY_SPEC_SELECT_ITEMS = QUERY_SPEC.nodesAttr("items");
  Attrs.Key<SQLNode> QUERY_SPEC_FROM = QUERY_SPEC.nodeAttr("from");
  Attrs.Key<SQLNode> QUERY_SPEC_WHERE = QUERY_SPEC.nodeAttr("where");
  Attrs.Key<List<SQLNode>> QUERY_SPEC_GROUP_BY = QUERY_SPEC.nodesAttr("groupBy");
  Attrs.Key<OLAPOption> QUERY_SPEC_OLAP_OPTION = QUERY_SPEC.attr("olapOption", OLAPOption.class);
  Attrs.Key<SQLNode> QUERY_SPEC_HAVING = QUERY_SPEC.nodeAttr("having");
  Attrs.Key<List<SQLNode>> QUERY_SPEC_WINDOWS = QUERY_SPEC.nodesAttr("windows");

  //// SelectItem
  Attrs.Key<SQLNode> SELECT_ITEM_EXPR = SELECT_ITEM.nodeAttr("expr");
  Attrs.Key<String> SELECT_ITEM_ALIAS = SELECT_ITEM.strAttr("alias");

  //// OrderItem
  Attrs.Key<SQLNode> ORDER_ITEM_EXPR = ORDER_ITEM.nodeAttr("expr");
  Attrs.Key<KeyDirection> ORDER_ITEM_DIRECTION = ORDER_ITEM.attr("direction", KeyDirection.class);

  //// GroupItem
  Attrs.Key<SQLNode> GROUP_ITEM_EXPR = GROUP_ITEM.nodeAttr("expr");

  //// WindowSpec
  Attrs.Key<String> WINDOW_SPEC_ALIAS = WINDOW_SPEC.strAttr("alias");
  Attrs.Key<String> WINDOW_SPEC_NAME = WINDOW_SPEC.strAttr("name");
  Attrs.Key<List<SQLNode>> WINDOW_SPEC_PARTITION = WINDOW_SPEC.nodesAttr("partition");
  Attrs.Key<List<SQLNode>> WINDOW_SPEC_ORDER = WINDOW_SPEC.nodesAttr("order");
  Attrs.Key<SQLNode> WINDOW_SPEC_FRAME = WINDOW_SPEC.nodeAttr("frame");

  //// WindowFrame
  Attrs.Key<WindowUnit> WINDOW_FRAME_UNIT = WINDOW_FRAME.attr("unit", WindowUnit.class);
  Attrs.Key<SQLNode> WINDOW_FRAME_START = WINDOW_FRAME.nodeAttr("start");
  Attrs.Key<SQLNode> WINDOW_FRAME_END = WINDOW_FRAME.nodeAttr("end");
  Attrs.Key<WindowExclusion> WINDOW_FRAME_EXCLUSION =
      WINDOW_FRAME.attr("exclusion", WindowExclusion.class);

  //// FrameBound
  Attrs.Key<SQLNode> FRAME_BOUND_EXPR = FRAME_BOUND.nodeAttr("expr");
  Attrs.Key<FrameBoundDirection> FRAME_BOUND_DIRECTION =
      FRAME_BOUND.attr("direction", FrameBoundDirection.class);

  //// IndexHint
  Attrs.Key<IndexHintType> INDEX_HINT_TYPE = INDEX_HINT.attr("type", IndexHintType.class);
  Attrs.Key<IndexHintTarget> INDEX_HINT_TARGET = INDEX_HINT.attr("target", IndexHintTarget.class);
  Attrs.Key<List<String>> INDEX_HINT_NAMES = INDEX_HINT.attr2("names", List.class);

  //// Statement
  Attrs.Key<StmtType> STATEMENT_TYPE = STATEMENT.attr("type", StmtType.class);
  Attrs.Key<SQLNode> STATEMENT_BODY = STATEMENT.nodeAttr("body");

  //// AlterSequence
  Attrs.Key<SQLNode> ALTER_SEQUENCE_NAME = ALTER_SEQUENCE.nodeAttr("name");
  Attrs.Key<String> ALTER_SEQUENCE_OPERATION = ALTER_SEQUENCE.strAttr("operation");
  Attrs.Key<Object> ALTER_SEQUENCE_PAYLOAD = ALTER_SEQUENCE.attr("payload", Object.class);

  //// AlterTable
  Attrs.Key<SQLNode> ALTER_TABLE_NAME = ALTER_TABLE.nodeAttr("name");
  Attrs.Key<List<SQLNode>> ALTER_TABLE_ACTIONS = ALTER_TABLE.nodesAttr("actions");

  //// AlterTableAction
  Attrs.Key<String> ALTER_TABLE_ACTION_NAME = ALTER_TABLE_ACTION.strAttr("name");
  Attrs.Key<Object> ALTER_TABLE_ACTION_PAYLOAD = ALTER_TABLE_ACTION.attr("payload", Object.class);

  //// Expr
  Attrs.Key<ExprType> EXPR_KIND = EXPR.attr("kind", ExprType.class);
  // for named argument in PG
  Attrs.Key<String> EXPR_FUNC_ARG_NAME = EXPR.strAttr("argName");
  Attrs.Key<Boolean> EXPR_FUNC_ARG_VARIADIC = EXPR.boolAttr("variadic");

  //// TableSource
  Attrs.Key<TableSourceType> TABLE_SOURCE_KIND = TABLE_SOURCE.attr("kind", TableSourceType.class);
}
