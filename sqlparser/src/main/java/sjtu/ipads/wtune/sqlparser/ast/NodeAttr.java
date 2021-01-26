package sjtu.ipads.wtune.sqlparser.ast;

import sjtu.ipads.wtune.common.attrs.AttrKey;
import sjtu.ipads.wtune.sqlparser.ast.constants.*;
import sjtu.ipads.wtune.sqlparser.ast.internal.NodeAttrBase;

import java.util.EnumSet;
import java.util.List;

import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.*;

public interface NodeAttr {
  AttrKey<NodeType> NODE_TYPE = NodeAttrBase.build("nodeType", NodeType.class);
  AttrKey<SQLNode> PARENT = NodeAttrBase.build("parent", SQLNode.class);

  //// TableName
  AttrKey<String> TABLE_NAME_SCHEMA = TABLE_NAME.strAttr("schema");
  AttrKey<String> TABLE_NAME_TABLE = TABLE_NAME.strAttr("table");

  //// ColumnName
  AttrKey<String> COLUMN_NAME_SCHEMA = COLUMN_NAME.strAttr("schema");
  AttrKey<String> COLUMN_NAME_TABLE = COLUMN_NAME.strAttr("table");
  AttrKey<String> COLUMN_NAME_COLUMN = COLUMN_NAME.strAttr("column");

  //// CommonName2
  AttrKey<String> NAME_2_0 = NAME_2.strAttr("part0");
  AttrKey<String> NAME_2_1 = NAME_2.strAttr("part1");

  //// CommonName3
  AttrKey<String> NAME_3_0 = NAME_3.strAttr("part0");
  AttrKey<String> NAME_3_1 = NAME_3.strAttr("part1");
  AttrKey<String> NAME_3_2 = NAME_3.strAttr("part2");

  //// CreateTable
  AttrKey<SQLNode> CREATE_TABLE_NAME = CREATE_TABLE.nodeAttr("name");
  AttrKey<List<SQLNode>> CREATE_TABLE_COLUMNS = CREATE_TABLE.nodesAttr("columns");
  AttrKey<List<SQLNode>> CREATE_TABLE_CONSTRAINTS = CREATE_TABLE.nodesAttr("constraints");
  AttrKey<String> CREATE_TABLE_ENGINE = CREATE_TABLE.strAttr("engine");

  //// ColumnDef
  AttrKey<SQLNode> COLUMN_DEF_NAME = COLUMN_DEF.nodeAttr("name");
  AttrKey<String> COLUMN_DEF_DATATYPE_RAW = COLUMN_DEF.strAttr("typeRaw");
  AttrKey<SQLDataType> COLUMN_DEF_DATATYPE = COLUMN_DEF.attr("dataType", SQLDataType.class);
  AttrKey<EnumSet<ConstraintType>> COLUMN_DEF_CONS = COLUMN_DEF.attr("constraint", EnumSet.class);
  AttrKey<SQLNode> COLUMN_DEF_REF = COLUMN_DEF.nodeAttr("references");
  AttrKey<Boolean> COLUMN_DEF_GENERATED = COLUMN_DEF.boolAttr("genearted");
  AttrKey<Boolean> COLUMN_DEF_DEFAULT = COLUMN_DEF.boolAttr("default");
  AttrKey<Boolean> COLUMN_DEF_AUTOINCREMENT = COLUMN_DEF.boolAttr("autoInc");

  //// References
  AttrKey<SQLNode> REFERENCES_TABLE = REFERENCES.nodeAttr("table");
  AttrKey<List<SQLNode>> REFERENCES_COLUMNS = REFERENCES.nodesAttr("columns");

  //// IndexDef
  AttrKey<String> INDEX_DEF_NAME = INDEX_DEF.strAttr("name");
  AttrKey<SQLNode> INDEX_DEF_TABLE = INDEX_DEF.nodeAttr("table");
  AttrKey<IndexType> INDEX_DEF_TYPE = INDEX_DEF.attr("type", IndexType.class);
  AttrKey<ConstraintType> INDEX_DEF_CONS = INDEX_DEF.attr("constraint", ConstraintType.class);
  AttrKey<List<SQLNode>> INDEX_DEF_KEYS = INDEX_DEF.nodesAttr("keys");
  AttrKey<SQLNode> INDEX_DEF_REFS = INDEX_DEF.nodeAttr("references");

  //// KeyPart
  AttrKey<String> KEY_PART_COLUMN = KEY_PART.strAttr("column");
  AttrKey<Integer> KEY_PART_LEN = KEY_PART.attr("length", Integer.class);
  AttrKey<SQLNode> KEY_PART_EXPR = KEY_PART.nodeAttr("expr");
  AttrKey<KeyDirection> KEY_PART_DIRECTION = KEY_PART.attr("direction", KeyDirection.class);

  //// Union
  AttrKey<SQLNode> SET_OP_LEFT = SET_OP.nodeAttr("left");
  AttrKey<SQLNode> SET_OP_RIGHT = SET_OP.nodeAttr("right");
  AttrKey<SetOperation> SET_OP_TYPE = SET_OP.attr("type", SetOperation.class);
  AttrKey<SetOperationOption> SET_OP_OPTION = SET_OP.attr("option", SetOperationOption.class);

  //// Query
  AttrKey<SQLNode> QUERY_BODY = QUERY.nodeAttr("body");
  AttrKey<List<SQLNode>> QUERY_ORDER_BY = QUERY.nodesAttr("orderBy");
  AttrKey<SQLNode> QUERY_LIMIT = QUERY.nodeAttr("limit");
  AttrKey<SQLNode> QUERY_OFFSET = QUERY.nodeAttr("offset");

  //// QuerySpec
  AttrKey<Boolean> QUERY_SPEC_DISTINCT = QUERY_SPEC.boolAttr("distinct");
  AttrKey<List<SQLNode>> QUERY_SPEC_DISTINCT_ON = QUERY_SPEC.nodesAttr("distinctOn");
  AttrKey<List<SQLNode>> QUERY_SPEC_SELECT_ITEMS = QUERY_SPEC.nodesAttr("items");
  AttrKey<SQLNode> QUERY_SPEC_FROM = QUERY_SPEC.nodeAttr("from");
  AttrKey<SQLNode> QUERY_SPEC_WHERE = QUERY_SPEC.nodeAttr("where");
  AttrKey<List<SQLNode>> QUERY_SPEC_GROUP_BY = QUERY_SPEC.nodesAttr("groupBy");
  AttrKey<OLAPOption> QUERY_SPEC_OLAP_OPTION = QUERY_SPEC.attr("olapOption", OLAPOption.class);
  AttrKey<SQLNode> QUERY_SPEC_HAVING = QUERY_SPEC.nodeAttr("having");
  AttrKey<List<SQLNode>> QUERY_SPEC_WINDOWS = QUERY_SPEC.nodesAttr("windows");

  //// SelectItem
  AttrKey<SQLNode> SELECT_ITEM_EXPR = SELECT_ITEM.nodeAttr("expr");
  AttrKey<String> SELECT_ITEM_ALIAS = SELECT_ITEM.strAttr("alias");

  //// OrderItem
  AttrKey<SQLNode> ORDER_ITEM_EXPR = ORDER_ITEM.nodeAttr("expr");
  AttrKey<KeyDirection> ORDER_ITEM_DIRECTION = ORDER_ITEM.attr("direction", KeyDirection.class);

  //// GroupItem
  AttrKey<SQLNode> GROUP_ITEM_EXPR = GROUP_ITEM.nodeAttr("expr");

  //// WindowSpec
  AttrKey<String> WINDOW_SPEC_ALIAS = WINDOW_SPEC.strAttr("alias");
  AttrKey<String> WINDOW_SPEC_NAME = WINDOW_SPEC.strAttr("name");
  AttrKey<List<SQLNode>> WINDOW_SPEC_PARTITION = WINDOW_SPEC.nodesAttr("partition");
  AttrKey<List<SQLNode>> WINDOW_SPEC_ORDER = WINDOW_SPEC.nodesAttr("order");
  AttrKey<SQLNode> WINDOW_SPEC_FRAME = WINDOW_SPEC.nodeAttr("frame");

  //// WindowFrame
  AttrKey<WindowUnit> WINDOW_FRAME_UNIT = WINDOW_FRAME.attr("unit", WindowUnit.class);
  AttrKey<SQLNode> WINDOW_FRAME_START = WINDOW_FRAME.nodeAttr("start");
  AttrKey<SQLNode> WINDOW_FRAME_END = WINDOW_FRAME.nodeAttr("end");
  AttrKey<WindowExclusion> WINDOW_FRAME_EXCLUSION =
      WINDOW_FRAME.attr("exclusion", WindowExclusion.class);

  //// FrameBound
  AttrKey<SQLNode> FRAME_BOUND_EXPR = FRAME_BOUND.nodeAttr("expr");
  AttrKey<FrameBoundDirection> FRAME_BOUND_DIRECTION =
      FRAME_BOUND.attr("direction", FrameBoundDirection.class);

  //// IndexHint
  AttrKey<IndexHintType> INDEX_HINT_TYPE = INDEX_HINT.attr("type", IndexHintType.class);
  AttrKey<IndexHintTarget> INDEX_HINT_TARGET = INDEX_HINT.attr("target", IndexHintTarget.class);
  AttrKey<List<String>> INDEX_HINT_NAMES = INDEX_HINT.attr("names", List.class);

  //// Statement
  AttrKey<StmtType> STATEMENT_TYPE = STATEMENT.attr("type", StmtType.class);
  AttrKey<SQLNode> STATEMENT_BODY = STATEMENT.nodeAttr("body");

  //// AlterSequence
  AttrKey<SQLNode> ALTER_SEQUENCE_NAME = ALTER_SEQUENCE.nodeAttr("name");
  AttrKey<String> ALTER_SEQUENCE_OPERATION = ALTER_SEQUENCE.strAttr("operation");
  AttrKey<Object> ALTER_SEQUENCE_PAYLOAD = ALTER_SEQUENCE.attr("payload", Object.class);

  //// AlterTable
  AttrKey<SQLNode> ALTER_TABLE_NAME = ALTER_TABLE.nodeAttr("name");
  AttrKey<List<SQLNode>> ALTER_TABLE_ACTIONS = ALTER_TABLE.nodesAttr("actions");

  //// AlterTableAction
  AttrKey<String> ALTER_TABLE_ACTION_NAME = ALTER_TABLE_ACTION.strAttr("name");
  AttrKey<Object> ALTER_TABLE_ACTION_PAYLOAD = ALTER_TABLE_ACTION.attr("payload", Object.class);

  //// Expr
  AttrKey<ExprType> EXPR_KIND = EXPR.attr("kind", ExprType.class);
  // for named argument in PG
  AttrKey<String> EXPR_FUNC_ARG_NAME = EXPR.strAttr("argName");
  AttrKey<Boolean> EXPR_FUNC_ARG_VARIADIC = EXPR.boolAttr("variadic");

  //// TableSource
  AttrKey<TableSourceType> TABLE_SOURCE_KIND = TABLE_SOURCE.attr("kind", TableSourceType.class);
}
