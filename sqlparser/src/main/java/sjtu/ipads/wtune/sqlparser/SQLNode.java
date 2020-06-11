package sjtu.ipads.wtune.sqlparser;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.common.utils.FuncUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static java.util.Collections.emptyList;

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
public class SQLNode implements Attrs, Cloneable {
  private String dbType;
  private Type type;
  private SQLNode parent;
  private List<SQLNode> children;

  public SQLNode() {}

  public SQLNode(Type type) {
    this.type = type;
  }

  public SQLNode(String dbType, Type type) {
    this.dbType = dbType;
    this.type = type;
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
    KEY_PART
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

  private static final String ATTR_PREFIX = "sql.attr";

  //// TableName
  public static final Key<String> TABLE_NAME_SCHEMA =
      Attrs.key(ATTR_PREFIX + ".tableName.schema", String.class);
  public static final Key<String> TABLE_NAME_TABLE =
      Attrs.key(ATTR_PREFIX + ".tableName.table", String.class);

  //// ColumnName
  public static final Key<String> COLUMN_NAME_SCHEMA =
      Attrs.key(ATTR_PREFIX + ".columnName.schema", String.class);
  public static final Key<String> COLUMN_NAME_TABLE =
      Attrs.key(ATTR_PREFIX + ".columnName.table", String.class);
  public static final Key<String> COLUMN_NAME_COLUMN =
      Attrs.key(ATTR_PREFIX + ".columnName.column", String.class);

  //// CreateTable
  public static final Key<SQLNode> CREATE_TABLE_NAME =
      Attrs.key(ATTR_PREFIX + ".createTable.name", SQLNode.class);
  public static final Key<List<SQLNode>> CREATE_TABLE_COLUMNS =
      Attrs.key2(ATTR_PREFIX + ".createTable.columns", List.class);
  public static final Key<List<SQLNode>> CREATE_TABLE_CONSTRAINTS =
      Attrs.key2(ATTR_PREFIX + ".createTable.constraints", List.class);
  public static final Key<String> CREATE_TABLE_ENGINE =
      Attrs.key(ATTR_PREFIX + ".createTable.engine", String.class);

  //// ColumnDef
  public static final Key<SQLNode> COLUMN_DEF_NAME =
      Attrs.key(ATTR_PREFIX + ".columnDef.name", SQLNode.class);
  public static final Key<String> COLUMN_DEF_DATATYPE_RAW =
      Attrs.key(ATTR_PREFIX + ".columnDef.dataTypeRaw", String.class);
  public static final Key<SQLDataType> COLUMN_DEF_DATATYPE =
      Attrs.key(ATTR_PREFIX + ".columnDef.dataType", SQLDataType.class);
  public static final Key<EnumSet<ConstraintType>> COLUMN_DEF_CONS =
      Attrs.key2(ATTR_PREFIX + ".columnDef.constraint", EnumSet.class);
  public static final Key<SQLNode> COLUMN_DEF_REF =
      Attrs.key(ATTR_PREFIX + ".columnDef.references", SQLNode.class);
  public static final Key<Boolean> COLUMN_DEF_GENERATED =
      Attrs.key(ATTR_PREFIX + ".columnDef.generated", Boolean.class);
  public static final Key<Boolean> COLUMN_DEF_DEFAULT =
      Attrs.key(ATTR_PREFIX + ".columnDef.default", Boolean.class);
  public static final Key<Boolean> COLUMN_DEF_AUTOINCREMENT =
      Attrs.key(ATTR_PREFIX + ".columnDef.autoIncrement", Boolean.class);

  //// References
  public static final Key<SQLNode> REFERENCES_TABLE =
      Attrs.key(ATTR_PREFIX + ".references.table", SQLNode.class);
  public static final Key<List<SQLNode>> REFERENCES_COLUMNS =
      Attrs.key2(ATTR_PREFIX + ".references.columns", List.class);

  //// Index
  public static final Key<String> INDEX_DEF_NAME =
      Attrs.key(ATTR_PREFIX + ".index.name", String.class);
  public static final Key<IndexType> INDEX_DEF_TYPE =
      Attrs.key(ATTR_PREFIX + ".index.type", IndexType.class);
  public static final Key<ConstraintType> INDEX_DEF_CONS =
      Attrs.key(ATTR_PREFIX + ".index.constraint", ConstraintType.class);
  public static final Key<List<SQLNode>> INDEX_DEF_KEYS =
      Attrs.key2(ATTR_PREFIX + ".index.keys", List.class);
  public static final Key<SQLNode> INDEX_DEF_REFS =
      Attrs.key(ATTR_PREFIX + ".index.references", SQLNode.class);

  //// KeyPart
  public static final Key<String> KEY_PART_COLUMN =
      Attrs.key(ATTR_PREFIX + ".keyPart.column", String.class);
  public static final Key<Integer> KEY_PART_LEN =
      Attrs.key(ATTR_PREFIX + ".keyPart.length", Integer.class);
  public static final Key<SQLNode> KEY_PART_EXPR =
      Attrs.key(ATTR_PREFIX + ".keyPart.expression", SQLNode.class);
  public static final Key<KeyDirection> KEY_PART_DIRECTION =
      Attrs.key(ATTR_PREFIX + ".index.direction", KeyDirection.class);
}
