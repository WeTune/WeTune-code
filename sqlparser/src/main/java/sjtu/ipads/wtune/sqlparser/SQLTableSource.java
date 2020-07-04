package sjtu.ipads.wtune.sqlparser;

import sjtu.ipads.wtune.common.attrs.Attrs;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.common.attrs.Attrs.Key.checkEquals;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.Kind.*;

public class SQLTableSource {
  public enum Kind {
    SIMPLE,
    JOINED,
    DERIVED;
    private final Set<String> attrs = new HashSet<>();

    public void addAttr(String attr) {
      attrs.add(attr);
    }
  }

  public enum JoinType {
    CROSS_JOIN("CROSS JOIN"),
    INNER_JOIN("INNER JOIN"),
    STRAIGHT_JOIN("STRAIGHT_JOIN"),
    LEFT_JOIN("LEFT JOIN"),
    RIGHT_JOIN("RIGHT JOIN"),
    NATURAL_INNER_JOIN("NATURAL JOIN"),
    NATURAL_LEFT_JOIN("NATURAL LEFT JOIN"),
    NATURAL_RIGHT_JOIN("NATURAL RIGHT JOIN");

    private final String text;

    JoinType(String text) {
      this.text = text;
    }

    public String text() {
      return text;
    }

    public boolean isInner() {
      return this == CROSS_JOIN
          || this == INNER_JOIN
          || this == STRAIGHT_JOIN
          || this == NATURAL_INNER_JOIN;
    }

    public boolean isNatural() {
      return this == NATURAL_INNER_JOIN || this == NATURAL_LEFT_JOIN || this == NATURAL_RIGHT_JOIN;
    }

    public boolean isOuter() {
      return this == LEFT_JOIN
          || this == RIGHT_JOIN
          || this == NATURAL_LEFT_JOIN
          || this == NATURAL_RIGHT_JOIN;
    }
  }

  public static final Attrs.Key<Kind> TABLE_SOURCE_KIND =
      Attrs.key(SQL_ATTR_PREFIX + ".expr.tableSource", Kind.class);

  public static SQLNode newTableSource(Kind kind) {
    final SQLNode node = new SQLNode(SQLNode.Type.TABLE_SOURCE);
    node.put(TABLE_SOURCE_KIND, kind);
    return node;
  }

  public static SQLNode simple(String name, String alias) {
    final SQLNode node = newTableSource(SIMPLE);
    node.put(SIMPLE_TABLE, tableName(name));
    node.put(SIMPLE_ALIAS, alias);
    return node;
  }

  public static SQLNode joined(SQLNode left, SQLNode right, JoinType type) {
    final SQLNode tableSource = newTableSource(JOINED);
    tableSource.put(JOINED_LEFT, left);
    tableSource.put(JOINED_RIGHT, right);
    tableSource.put(JOINED_TYPE, type);
    return tableSource;
  }

  public static boolean isJoined(SQLNode node) {
    return node.type() == SQLNode.Type.TABLE_SOURCE && node.get(TABLE_SOURCE_KIND) == JOINED;
  }

  public static boolean isDerived(SQLNode node) {
    return node.type() == SQLNode.Type.TABLE_SOURCE && node.get(TABLE_SOURCE_KIND) == DERIVED;
  }

  public static boolean isSimple(SQLNode node) {
    return node.type() == SQLNode.Type.TABLE_SOURCE && node.get(TABLE_SOURCE_KIND) == SIMPLE;
  }

  static final String EXPR_ATTR_PREFIX = SQL_ATTR_PREFIX + ".tableSource.";

  private static String attrPrefix(Kind kind) {
    return EXPR_ATTR_PREFIX + kind.name().toLowerCase() + ".";
  }

  private static <T> Attrs.Key<T> attr(Kind kind, String name, Class<T> clazz) {
    final Attrs.Key<T> attr = Attrs.key(attrPrefix(kind) + name, clazz);
    attr.addCheck(checkEquals(TABLE_SOURCE_KIND, kind));
    kind.addAttr(attr.name());
    return attr;
  }

  private static <T> Attrs.Key<T> attr2(Kind kind, String name, Class<?> clazz) {
    final Attrs.Key<T> attr = Attrs.key2(attrPrefix(kind) + name, clazz);
    attr.addCheck(checkEquals(TABLE_SOURCE_KIND, kind));
    kind.addAttr(attr.name());
    return attr;
  }

  private static Attrs.Key<String> stringAttr(Kind kind, String name) {
    return attr(kind, name, String.class);
  }

  private static Attrs.Key<Boolean> booleanAttr(Kind kind, String name) {
    return attr(kind, name, Boolean.class);
  }

  private static Attrs.Key<SQLNode> nodeAttr(Kind kind, String name) {
    return attr(kind, name, SQLNode.class);
  }

  private static Attrs.Key<List<SQLNode>> nodesAttr(Kind kind, String name) {
    return attr2(kind, name, List.class);
  }

  public static String tableSourceName(SQLNode node) {
    if (node.type() != SQLNode.Type.TABLE_SOURCE) return null;
    switch (node.get(TABLE_SOURCE_KIND)) {
      case SIMPLE:
        {
          final String alias = node.get(SIMPLE_ALIAS);
          return alias != null ? alias : node.get(SIMPLE_TABLE).get(TABLE_NAME_TABLE);
        }
      case DERIVED:
        return node.get(DERIVED_ALIAS);
      default:
        return null;
    }
  }

  //// Simple
  public static final Attrs.Key<SQLNode> SIMPLE_TABLE = nodeAttr(SIMPLE, "table");
  public static final Attrs.Key<List<String>> SIMPLE_PARTITIONS =
      attr2(SIMPLE, "partitions", List.class);
  public static final Attrs.Key<String> SIMPLE_ALIAS = stringAttr(SIMPLE, "alias");
  public static final Attrs.Key<List<SQLNode>> SIMPLE_HINTS = nodesAttr(SIMPLE, "hints");

  //// Joined
  public static final Attrs.Key<SQLNode> JOINED_LEFT = nodeAttr(JOINED, "left");
  public static final Attrs.Key<SQLNode> JOINED_RIGHT = nodeAttr(JOINED, "right");
  public static final Attrs.Key<JoinType> JOINED_TYPE = attr(JOINED, "type", JoinType.class);
  public static final Attrs.Key<SQLNode> JOINED_ON = nodeAttr(JOINED, "on");
  public static final Attrs.Key<List<String>> JOINED_USING = attr2(JOINED, "using", List.class);

  //// Derived
  public static final Attrs.Key<SQLNode> DERIVED_SUBQUERY = nodeAttr(DERIVED, "subquery");
  public static final Attrs.Key<String> DERIVED_ALIAS = stringAttr(DERIVED, "alias");
  public static final Attrs.Key<Boolean> DERIVED_LATERAL = booleanAttr(DERIVED, "lateral");
  public static final Attrs.Key<List<String>> DERIVED_INTERNAL_REFS =
      attr2(DERIVED, "internalRefs", List.class);
}
