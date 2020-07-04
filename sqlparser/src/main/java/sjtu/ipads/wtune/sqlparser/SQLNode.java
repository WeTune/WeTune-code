package sjtu.ipads.wtune.sqlparser;

import sjtu.ipads.wtune.common.attrs.Attrs;

import java.lang.System.Logger.Level;
import java.util.*;

import static java.util.Collections.emptyList;
import static sjtu.ipads.wtune.common.attrs.Attrs.Key.checkAgainst;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.EXPR_ATTR_PREFIX;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.EXPR_KIND;
import static sjtu.ipads.wtune.sqlparser.SQLNode.Type.*;

/**
 * A general representation of syntax struct of SQL statement.
 *
 * <p>We choose to use a single class + attributes instead of interface + subclass for better
 * adaptability.
 *
 * <p>Currently, we tailor the parser's capability to just serve for our purpose. Thus, some
 * elements are not considered.
 *
 * <h2>AST Definition</h2>
 *
 * <h3>Enums</h3>
 *
 * <ul>
 *   <li>{@link Type}
 *   <li>{@link ConstraintType}
 *   <li>{@link IndexType}
 *   <li>{@link KeyDirection}
 * </ul>
 *
 * <h3>AST Nodes</h3>
 *
 * <pre>{@code
 * TABLE_NAME
 * | SCHEMA: String
 * | TABLE: String
 * }</pre>
 *
 * <pre>{@code
 * COLUMN_NAME
 * | SCHEMA: String
 * | TABLE: String
 * | COLUMN: String
 * }</pre>
 *
 * <pre>{@code
 * CREATE_TABLE
 * | NAME: SQLNode<TABLE_NAME>
 * | ENGINE: String
 * | COLUMNS: [SQLNode<COLUMN_DEF>]
 * | CONSTRAINTS: [SQLNode<INDEX_DEF>]
 * }</pre>
 *
 * <pre>{@code
 * COLUMN_DEF
 * | NAME: SQLNode<COLUMN_NAME>
 * | DATATYPE_RAW: String
 * | DATATYPE: SQLDataType
 * | CONS: EnumSet of Constraint
 * | REF: SQLNode<REFERENCES>
 * | GENERATED: boolean
 * | DEFAULT: boolean
 * | AUTOINCREMENT: boolean
 * }</pre>
 *
 * <pre>{@code
 * REFERENCES
 * | TABLE: SQLNode<TABLE_NAME>
 * | COLUMNS: [SQLNode<COLUMN_NAME>]
 * }</pre>
 *
 * <pre>{@code
 * INDEX_DEF
 * | NAME: String
 * | TYPE: IndexType
 * | CONS: Constraint
 * | KEYS: [SQLNode<KEY_PART>]
 * | REFS: SQLNode<REFERENCES>
 * }</pre>
 *
 * <pre>{@code
 * KEY_PART
 * | COLUMN: String
 * | LEN: int
 * | DIRECTION: KeyDirection
 * }</pre>
 */
public class SQLNode implements Attrs<SQLNode>, Cloneable {
  private static final System.Logger LOG = System.getLogger("SQL.Core");

  private final Map<String, Object> directAttrs = new HashMap<>();

  @Override
  public Map<String, Object> directAttrs() {
    return directAttrs;
  }

  public SQLVisitor currentMutator = null;
  private boolean structChanged = false;

  private String dbType;
  private Type type;
  private SQLNode parent;
  private List<SQLNode> children;

  private SQLNode() {}

  public SQLNode(Type type) {
    this();
    this.type = type;
  }

  public SQLNode(String dbType, Type type) {
    this();
    this.dbType = dbType;
    this.type = type;
  }

  @Override
  public <T> T checkFailed(Key<T> key) {
    if (type == EXPR && key.name().startsWith(EXPR_ATTR_PREFIX))
      LOG.log(Level.WARNING, "mismatching attr {0} on {1}", key, get(EXPR_KIND));
    else LOG.log(Level.WARNING, "mismatching attr {0} on {1}", key, type());

    return null;
  }

  public String dbType() {
    return dbType;
  }

  public Type type() {
    return type;
  }

  public SQLNode parent() {
    return parent;
  }

  public List<SQLNode> children() {
    return children == null ? emptyList() : children;
  }

  public void setParent(SQLNode parent) {
    this.parent = parent;
  }

  public void setChildren(List<SQLNode> children) {
    this.children = children;
    if (children != null) children.forEach(it -> it.setParent(this));
  }

  @SuppressWarnings({"unchecked", "raw"})
  public void replaceThis(SQLNode replacement) {
    //    dbType = replacement.dbType;
    type = replacement.type;
    setChildren(new ArrayList<>(replacement.children()));

    final var directAttrs = directAttrs();
    directAttrs.clear();
    directAttrs.putAll(replacement.directAttrs());
  }

  public boolean structChanged() {
    return structChanged;
  }

  public void flagStructChanged(boolean flag) {
    structChanged = flag;
  }

  public SQLNode relink() {
    final List<SQLNode> children = new ArrayList<>();

    for (var e : ofPrefix(SQL_ATTR_PREFIX).entrySet()) {
      final Object value = e.getValue();
      if (value instanceof SQLNode) children.add((SQLNode) value);
      if (value instanceof List)
        for (Object o : (List<?>) value) if (o instanceof SQLNode) children.add((SQLNode) o);
    }

    setChildren(children);
    return this;
  }

  public SQLNode relinkAll() {
    relink();
    children().forEach(SQLNode::relinkAll);
    return this;
  }

  /** Note: parent is not set. */
  public SQLNode copy() {
    return CopyVisitor.doCopy(this);
  }

  SQLNode copy0() {
    final SQLNode newNode = new SQLNode(this.type);
    newNode.directAttrs().putAll(this.directAttrs());
    return newNode;
  }

  public void accept(SQLVisitor visitor) {
    final boolean isMutator = visitor.isMutator();
    if (isMutator && currentMutator != null && currentMutator != visitor)
      throw new ConcurrentModificationException();

    if (isMutator) {
      currentMutator = visitor;
      structChanged = false;
    }

    final boolean visitChildren = VisitorController.enter(this, visitor);

    if (isMutator && structChanged) {
      // struct changed, re-visit this
      accept(visitor);
      return;
    }

    if (visitChildren) {
      VisitorController.visitChildren(this, visitor);

      if (isMutator && structChanged) {
        // struct changed, re-visit
        accept(visitor);
        return;
      }
    }

    VisitorController.leave(this, visitor);

    if (isMutator && structChanged)
      // struct changed, re-visit
      accept(visitor);

    if (isMutator) currentMutator = null;
  }

  public String toString() {
    return toString(true);
  }

  public String toString(boolean singleLine) {
    final SQLFormatter formatter = new SQLFormatter(singleLine);
    accept(formatter);
    return formatter.toString();
  }

  public void invalidate() {
    type = INVALID;
    structChanged = true;
    children.clear();
    directAttrs().clear();
  }

  public static final String MYSQL = "mysql";
  public static final String POSTGRESQL = "postgresql";

  public enum Type {
    INVALID,
    COMMON_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    CREATE_TABLE,
    ALTER_SEQUENCE,
    ALTER_TABLE,
    ALTER_TABLE_ACTION,
    COLUMN_DEF,
    REFERENCES,
    INDEX_DEF,
    KEY_PART,
    UNION,
    QUERY,
    QUERY_SPEC,
    SELECT_ITEM,
    EXPR,
    ORDER_ITEM,
    WINDOW_SPEC,
    WINDOW_FRAME,
    FRAME_BOUND,
    TABLE_SOURCE,
    INDEX_HINT,
    STATEMENT;

    private final Set<String> attrs = new HashSet<>();

    public void addAttr(String attr) {
      attrs.add(attr);
    }

    public boolean isValidAttr(String name) {
      return attrs.contains(name);
    }
  }

  public enum ConstraintType {
    UNIQUE,
    PRIMARY,
    NOT_NULL,
    FOREIGN,
    CHECK
  }

  public enum IndexType {
    BTREE,
    RTREE,
    HASH,
    FULLTEXT,
    SPATIAL,
    GIST,
    SPGIST,
    GIN,
    BRIN
  }

  public enum KeyDirection {
    ASC,
    DESC
  }

  public enum WindowUnit {
    ROWS,
    RANGE,
    GROUPS;

    private final String text = name();

    public String text() {
      return text;
    }
  }

  public enum WindowExclusion {
    CURRENT_ROW("CURRENT ROW"),
    GROUP("GROUP"),
    TIES("TIES"),
    NO_OTHERS("NO OTHERS");

    private final String text;

    WindowExclusion(String text) {
      this.text = text;
    }

    public String text() {
      return text;
    }
  }

  public enum FrameBoundDirection {
    PRECEDING,
    FOLLOWING;

    public static FrameBoundDirection ofText(String text) {
      if (text.equalsIgnoreCase(PRECEDING.name())) return PRECEDING;
      else if (text.equalsIgnoreCase(FOLLOWING.name())) return FOLLOWING;
      else return null;
    }
  }

  public enum UnionOption {
    DISTINCT,
    ALL
  }

  public enum OLAPOption {
    WITH_ROLLUP("WITH ROLLUP"),
    WITH_CUBE("WITH CUBE");
    private final String text;

    OLAPOption(String text) {
      this.text = text;
    }

    public String text() {
      return text;
    }
  }

  public enum IndexHintType {
    FORCE,
    IGNORE,
    USE
  }

  public enum IndexHintTarget {
    JOIN("JOIN"),
    ORDER_BY("ORDER BY"),
    GROUP_BY("GROUP BY");
    private final String text;

    IndexHintTarget(String text) {
      this.text = text;
    }

    public String text() {
      return text;
    }
  }

  public enum StmtType {
    SELECT,
    UPDATE,
    INSERT,
    DELETE
  }

  static final String SQL_ATTR_PREFIX = "sql.attr.";

  public static SQLNode tableName(String name) {
    final SQLNode node = new SQLNode(TABLE_NAME);
    node.put(TABLE_NAME_TABLE, name);
    return node;
  }

  public static SQLNode columnName(String table, String column) {
    final SQLNode node = new SQLNode(COLUMN_NAME);
    node.put(COLUMN_NAME_TABLE, table);
    node.put(COLUMN_NAME_COLUMN, column);
    return node;
  }

  public static SQLNode selectItem(SQLNode expr, String alias) {
    final SQLNode node = new SQLNode(SELECT_ITEM);
    node.put(SELECT_ITEM_EXPR, expr);
    node.put(SELECT_ITEM_ALIAS, alias);
    return node;
  }

  private static <T> Key<T> attr(Type nodeType, String name, Class<T> clazz) {
    final Key<T> attr =
        Attrs.key(SQL_ATTR_PREFIX + nodeType.name().toLowerCase() + "." + name, clazz);
    attr.setCheck(checkAgainst(SQLNode.class, it -> it.type() == nodeType));
    nodeType.addAttr(attr.name());
    return attr;
  }

  private static <T> Key<T> attr2(Type nodeType, String name, Class<?> clazz) {
    final Key<T> attr =
        Attrs.key2(SQL_ATTR_PREFIX + nodeType.name().toLowerCase() + "." + name, clazz);
    attr.setCheck(checkAgainst(SQLNode.class, it -> it.type() == nodeType));
    nodeType.addAttr(attr.name());
    return attr;
  }

  private static Key<String> stringAttr(Type nodeType, String name) {
    return attr(nodeType, name, String.class);
  }

  private static Key<Boolean> booleanAttr(Type nodeType, String name) {
    return attr(nodeType, name, Boolean.class);
  }

  private static Key<SQLNode> nodeAttr(Type nodeType, String name) {
    return attr(nodeType, name, SQLNode.class);
  }

  private static Key<List<SQLNode>> nodesAttr(Type nodeType, String name) {
    return attr2(nodeType, name, List.class);
  }

  //// TableName
  public static final Key<String> TABLE_NAME_SCHEMA = stringAttr(TABLE_NAME, "schema");
  public static final Key<String> TABLE_NAME_TABLE = stringAttr(TABLE_NAME, "table");

  //// ColumnName
  public static final Key<String> COLUMN_NAME_SCHEMA = stringAttr(COLUMN_NAME, "schema");
  public static final Key<String> COLUMN_NAME_TABLE = stringAttr(COLUMN_NAME, "table");
  public static final Key<String> COLUMN_NAME_COLUMN = stringAttr(COLUMN_NAME, "column");

  //// ColumnName
  public static final Key<String> COMMON_NAME_0 = stringAttr(COMMON_NAME, "part0");
  public static final Key<String> COMMON_NAME_1 = stringAttr(COMMON_NAME, "part1");
  public static final Key<String> COMMON_NAME_2 = stringAttr(COMMON_NAME, "part2");

  //// CreateTable
  public static final Key<SQLNode> CREATE_TABLE_NAME = nodeAttr(CREATE_TABLE, "name");
  public static final Key<List<SQLNode>> CREATE_TABLE_COLUMNS = nodesAttr(CREATE_TABLE, "columns");
  public static final Key<List<SQLNode>> CREATE_TABLE_CONSTRAINTS =
      nodesAttr(CREATE_TABLE, "constraints");
  public static final Key<String> CREATE_TABLE_ENGINE = stringAttr(CREATE_TABLE, "engine");

  //// ColumnDef
  public static final Key<SQLNode> COLUMN_DEF_NAME = nodeAttr(COLUMN_DEF, "name");
  public static final Key<String> COLUMN_DEF_DATATYPE_RAW = stringAttr(COLUMN_DEF, "typeRaw");
  public static final Key<SQLDataType> COLUMN_DEF_DATATYPE =
      attr(COLUMN_DEF, "dataType", SQLDataType.class);
  public static final Key<EnumSet<ConstraintType>> COLUMN_DEF_CONS =
      attr2(COLUMN_DEF, "constraint", EnumSet.class);
  public static final Key<SQLNode> COLUMN_DEF_REF = nodeAttr(COLUMN_DEF, "references");
  public static final Key<Boolean> COLUMN_DEF_GENERATED = booleanAttr(COLUMN_DEF, "genearted");
  public static final Key<Boolean> COLUMN_DEF_DEFAULT = booleanAttr(COLUMN_DEF, "default");
  public static final Key<Boolean> COLUMN_DEF_AUTOINCREMENT = booleanAttr(COLUMN_DEF, "autoInc");

  //// References
  public static final Key<SQLNode> REFERENCES_TABLE = nodeAttr(REFERENCES, "table");
  public static final Key<List<SQLNode>> REFERENCES_COLUMNS = nodesAttr(REFERENCES, "columns");

  //// IndexDef
  public static final Key<String> INDEX_DEF_NAME = stringAttr(INDEX_DEF, "name");
  public static final Key<SQLNode> INDEX_DEF_TABLE = nodeAttr(INDEX_DEF, "table");
  public static final Key<IndexType> INDEX_DEF_TYPE = attr(INDEX_DEF, "type", IndexType.class);
  public static final Key<ConstraintType> INDEX_DEF_CONS =
      attr(INDEX_DEF, "constraint", ConstraintType.class);
  public static final Key<List<SQLNode>> INDEX_DEF_KEYS = nodesAttr(INDEX_DEF, "keys");
  public static final Key<SQLNode> INDEX_DEF_REFS = nodeAttr(INDEX_DEF, "references");

  //// KeyPart
  public static final Key<String> KEY_PART_COLUMN = stringAttr(KEY_PART, "column");
  public static final Key<Integer> KEY_PART_LEN = attr(KEY_PART, "length", Integer.class);
  public static final Key<SQLNode> KEY_PART_EXPR = nodeAttr(KEY_PART, "expr");
  public static final Key<KeyDirection> KEY_PART_DIRECTION =
      attr(KEY_PART, "direction", KeyDirection.class);

  //// Union
  public static final Key<SQLNode> UNION_LEFT = nodeAttr(UNION, "left");
  public static final Key<SQLNode> UNION_RIGHT = nodeAttr(UNION, "right");
  public static final Key<UnionOption> UNION_OPTION = attr(UNION, "option", UnionOption.class);

  //// Query
  public static final Key<SQLNode> QUERY_BODY = nodeAttr(QUERY, "body");
  public static final Key<List<SQLNode>> QUERY_ORDER_BY = nodesAttr(QUERY, "orderBy");
  public static final Key<SQLNode> QUERY_LIMIT = nodeAttr(QUERY, "limit");
  public static final Key<SQLNode> QUERY_OFFSET = nodeAttr(QUERY, "offset");

  //// QuerySpec
  public static final Key<Boolean> QUERY_SPEC_DISTINCT = booleanAttr(QUERY_SPEC, "distinct");
  public static final Key<List<SQLNode>> QUERY_SPEC_SELECT_ITEMS = nodesAttr(QUERY_SPEC, "items");
  public static final Key<SQLNode> QUERY_SPEC_FROM = nodeAttr(QUERY_SPEC, "from");
  public static final Key<SQLNode> QUERY_SPEC_WHERE = nodeAttr(QUERY_SPEC, "where");
  public static final Key<List<SQLNode>> QUERY_SPEC_GROUP_BY = nodesAttr(QUERY_SPEC, "groupBy");
  public static final Key<OLAPOption> QUERY_SPEC_OLAP_OPTION =
      attr(QUERY_SPEC, "olapOption", OLAPOption.class);
  public static final Key<SQLNode> QUERY_SPEC_HAVING = nodeAttr(QUERY_SPEC, "having");
  public static final Key<List<SQLNode>> QUERY_SPEC_WINDOWS = nodesAttr(QUERY_SPEC, "windows");

  //// SelectItem
  public static final Key<SQLNode> SELECT_ITEM_EXPR = nodeAttr(SELECT_ITEM, "expr");
  public static final Key<String> SELECT_ITEM_ALIAS = stringAttr(SELECT_ITEM, "alias");

  //// OrderItem
  public static final Key<SQLNode> ORDER_ITEM_EXPR = nodeAttr(ORDER_ITEM, "expr");
  public static final Key<KeyDirection> ORDER_ITEM_DIRECTION =
      attr(ORDER_ITEM, "direction", KeyDirection.class);

  //// WindowSpec
  public static final Key<String> WINDOW_SPEC_ALIAS = stringAttr(WINDOW_SPEC, "alias");
  public static final Key<String> WINDOW_SPEC_NAME = stringAttr(WINDOW_SPEC, "name");
  public static final Key<List<SQLNode>> WINDOW_SPEC_PARTITION =
      nodesAttr(WINDOW_SPEC, "partition");
  public static final Key<List<SQLNode>> WINDOW_SPEC_ORDER = nodesAttr(WINDOW_SPEC, "order");
  public static final Key<SQLNode> WINDOW_SPEC_FRAME = nodeAttr(WINDOW_SPEC, "frame");

  //// WindowFrame
  public static final Key<WindowUnit> WINDOW_FRAME_UNIT =
      attr(WINDOW_FRAME, "unit", WindowUnit.class);
  public static final Key<SQLNode> WINDOW_FRAME_START = nodeAttr(WINDOW_FRAME, "start");
  public static final Key<SQLNode> WINDOW_FRAME_END = nodeAttr(WINDOW_FRAME, "end");
  public static final Key<WindowExclusion> WINDOW_FRAME_EXCLUSION =
      attr(WINDOW_FRAME, "exclusion", WindowExclusion.class);

  //// FrameBound
  public static final Key<SQLNode> FRAME_BOUND_EXPR = nodeAttr(FRAME_BOUND, "expr");
  public static final Key<FrameBoundDirection> FRAME_BOUND_DIRECTION =
      attr(FRAME_BOUND, "direction", FrameBoundDirection.class);

  //// IndexHint
  public static final Key<IndexHintType> INDEX_HINT_TYPE =
      attr(INDEX_HINT, "type", IndexHintType.class);
  public static final Key<IndexHintTarget> INDEX_HINT_TARGET =
      attr(INDEX_HINT, "target", IndexHintTarget.class);
  public static final Key<List<String>> INDEX_HINT_NAMES = attr2(INDEX_HINT, "names", List.class);

  //// Statement
  public static final Key<StmtType> STATEMENT_TYPE = attr(STATEMENT, "type", StmtType.class);
  public static final Key<SQLNode> STATEMENT_BODY = nodeAttr(STATEMENT, "body");

  //// AlterSequence
  public static final Key<SQLNode> ALTER_SEQUENCE_NAME = nodeAttr(ALTER_SEQUENCE, "name");
  public static final Key<String> ALTER_SEQUENCE_OPERATION =
      stringAttr(ALTER_SEQUENCE, "operation");
  public static final Key<Object> ALTER_SEQUENCE_PAYLOAD =
      attr(ALTER_SEQUENCE, "payload", Object.class);

  //// AlterTable
  public static final Key<SQLNode> ALTER_TABLE_NAME = nodeAttr(ALTER_TABLE, "name");
  public static final Key<List<SQLNode>> ALTER_TABLE_ACTIONS = nodesAttr(ALTER_TABLE, "actions");

  //// AlterTableAction
  public static final Key<String> ALTER_TABLE_ACTION_NAME = stringAttr(ALTER_TABLE_ACTION, "name");
  public static final Key<Object> ALTER_TABLE_ACTION_PAYLOAD =
      attr(ALTER_TABLE_ACTION, "payload", Object.class);
}
