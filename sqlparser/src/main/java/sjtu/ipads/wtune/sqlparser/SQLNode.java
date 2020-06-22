package sjtu.ipads.wtune.sqlparser;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.common.utils.FuncUtils;

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

  private String dbType;
  private Type type;
  private SQLNode parent;
  private List<SQLNode> children;
  private Map<String, Object> attrs;

  public SQLNode() {
    attrs = directAttrs(); // for debug use
  }

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

  public SQLNode relink() {
    final List<SQLNode> children = new ArrayList<>();

    for (var e : ofPrefix(ATTR_PREFIX).entrySet()) {
      final var value = e.getValue();
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

  public SQLNode copy() {
    final var newNode = new SQLNode();
    newNode.directAttrs().putAll(this.directAttrs());

    final var newChildren = FuncUtils.listMap(SQLNode::copy, children());
    newNode.setChildren(newChildren);

    return newNode;
  }

  public void accept(SQLVisitor visitor) {
    if (VisitorController.enter(this, visitor)) VisitorController.visitChildren(this, visitor);
    VisitorController.leave(this, visitor);
  }

  public String toString() {
    return toString(true);
  }

  public String toString(boolean singleLine) {
    final SQLFormatter formatter = new SQLFormatter(singleLine);
    accept(formatter);
    return formatter.toString();
  }

  public static final String MYSQL = "mysql";
  public static final String POSTGRESQL = "postgresql";

  public enum Type {
    INVALID,
    TABLE_NAME,
    COLUMN_NAME,
    CREATE_TABLE,
    COLUMN_DEF,
    REFERENCES,
    INDEX_DEF,
    KEY_PART,
    QUERY,
    QUERY_SPEC,
    SELECT_ITEM,
    EXPR,
    ORDER_ITEM,
    WINDOW_SPEC,
    WINDOW_FRAME,
    FRAME_BOUND;

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
    SPATIAL
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

  static final String ATTR_PREFIX = "sql.attr";

  private static <T> Key<T> attr(Type nodeType, String name, Class<T> clazz) {
    final Key<T> attr = Attrs.key(ATTR_PREFIX + nodeType.name().toLowerCase() + name, clazz);
    attr.setCheck(checkAgainst(SQLNode.class, it -> it.type() == nodeType));
    nodeType.addAttr(attr.name());
    return attr;
  }

  private static <T> Key<T> attr2(Type nodeType, String name, Class<?> clazz) {
    final Key<T> attr = Attrs.key2(ATTR_PREFIX + nodeType.name().toLowerCase() + name, clazz);
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

  //// Index
  public static final Key<String> INDEX_DEF_NAME = stringAttr(INDEX_DEF, "name");
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

  //// Query
  public static final Key<SQLNode> QUERY_BODY = nodeAttr(QUERY, "body");

  //// QuerySpec
  public static final Key<Boolean> QUERY_SPEC_DISTINCT = booleanAttr(QUERY_SPEC, "distinct");
  public static final Key<List<SQLNode>> QUERY_SPEC_SELECT_ITEMS = nodesAttr(QUERY_SPEC, "items");

  //// SelectItem
  public static final Key<SQLNode> SELECT_ITEM_EXPR = nodeAttr(SELECT_ITEM, "expr");
  public static final Key<String> SELECT_ITEM_ALIAS = stringAttr(SELECT_ITEM, "alias");

  //// OrderItem
  public static final Key<SQLNode> ORDER_ITEM_EXPR = nodeAttr(ORDER_ITEM, "expr");
  public static final Key<KeyDirection> ORDER_ITEM_DIRECTION =
      attr(ORDER_ITEM, "direction", KeyDirection.class);

  //// WindowSpec
  public static final Key<String> WINDOW_SPEC_NAME = stringAttr(WINDOW_SPEC, "expr");
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
}
