package sjtu.ipads.wtune.sqlparser.mysql;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.SQLDataCategory;
import sjtu.ipads.wtune.sqlparser.SQLDataType;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.common.utils.FuncUtils;

import java.util.List;
import java.util.Set;

import static sjtu.ipads.wtune.common.utils.Commons.unquoted;
import static sjtu.ipads.wtune.sqlparser.SQLDataType.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.KeyDirection.ASC;
import static sjtu.ipads.wtune.sqlparser.SQLNode.KeyDirection.DESC;
import static java.util.Collections.emptyList;

public interface MySQLASTHelper {
  static String stringifyText(MySQLParser.TextStringLiteralContext text) {
    if (text.SINGLE_QUOTED_TEXT() != null) return unquoted(text.value.getText(), '\'');
    else if (text.DOUBLE_QUOTED_TEXT() != null) return unquoted(text.value.getText(), '"');
    else {
      assert false;
      return null;
    }
  }

  static String stringifyText(MySQLParser.TextOrIdentifierContext text) {
    if (text.identifier() != null) return stringifyIdentifier(text.identifier());
    else return stringifyText(text.textStringLiteral());
  }

  static String stringifyIdentifier(MySQLParser.PureIdentifierContext id) {
    if (id == null) return null;

    if (id.IDENTIFIER() != null) {
      return id.IDENTIFIER().getText();

    } else if (id.BACK_TICK_QUOTED_ID() != null) {
      return unquoted(id.BACK_TICK_QUOTED_ID().getText(), '`');

    } else if (id.DOUBLE_QUOTED_TEXT() != null) {
      return unquoted(id.DOUBLE_QUOTED_TEXT().getText(), '"');

    } else {
      assert false;
      return null;
    }
  }

  static String stringifyIdentifier(MySQLParser.IdentifierContext id) {
    return id == null
        ? null
        : id.pureIdentifier() != null ? stringifyIdentifier(id.pureIdentifier()) : id.getText();
  }

  static String stringifyIdentifier(MySQLParser.DotIdentifierContext id) {
    return id == null ? null : stringifyIdentifier(id.identifier());
  }

  /** @return string[2] */
  static String[] stringifyIdentifier(MySQLParser.QualifiedIdentifierContext id) {
    final var part0 = stringifyIdentifier(id.identifier());
    final var part1 = stringifyIdentifier(id.dotIdentifier());
    assert part0 != null;
    // <qualifier, identifier>
    return part1 == null ? new String[] {null, part0} : new String[] {part0, part1};
  }

  /** @return string[3] */
  static String[] stringifyIdentifier(MySQLParser.FieldIdentifierContext id) {
    final var qualifiedPart = id.qualifiedIdentifier();
    final var dotPart = id.dotIdentifier();

    if (qualifiedPart != null) {
      final var qualifiedId = stringifyIdentifier(qualifiedPart);
      final var dotId = stringifyIdentifier(dotPart);

      if (dotId == null) return new String[] {null, qualifiedId[0], qualifiedId[1]};
      else return new String[] {qualifiedId[0], qualifiedId[1], dotId};
    } else {
      assert dotPart != null;

      return new String[] {null, null, stringifyIdentifier(dotPart)};
    }
  }

  static SQLNode tableName(
      MySQLParser.QualifiedIdentifierContext qualifiedId, MySQLParser.DotIdentifierContext dotId) {
    final var node = new SQLNode(Type.TABLE_NAME);
    final String schema, table;

    if (qualifiedId != null) {
      final var pair = stringifyIdentifier(qualifiedId);
      schema = pair[0];
      table = pair[1];

    } else if (dotId != null) {
      schema = null;
      table = stringifyIdentifier(dotId);

    } else {
      assert false;
      return null;
    }

    node.put(SQLNode.TABLE_NAME_SCHEMA, schema);
    node.put(SQLNode.TABLE_NAME_TABLE, table);

    return node;
  }

  static void collectColumnAttrs(List<MySQLParser.ColumnAttributeContext> attrs, Attrs out) {
    if (attrs == null) return;
    attrs.forEach(attr -> collectColumnAttr(attr, out));
  }

  static void collectGColumnAttrs(List<MySQLParser.GcolAttributeContext> attrs, Attrs out) {
    if (attrs == null) return;
    attrs.forEach(attr -> collectGColumnAttr(attr, out));
  }

  static void collectColumnAttr(MySQLParser.ColumnAttributeContext attrs, Attrs out) {
    if (attrs.NOT_SYMBOL() != null && attrs.nullLiteral() != null)
      out.flag(COLUMN_DEF_CONS, Constraint.NOT_NULL);
    if (attrs.UNIQUE_SYMBOL() != null) out.flag(COLUMN_DEF_CONS, Constraint.UNIQUE);
    if (attrs.PRIMARY_SYMBOL() != null) out.flag(COLUMN_DEF_CONS, Constraint.PRIMARY);
    if (attrs.checkConstraint() != null) out.flag(COLUMN_DEF_CONS, Constraint.CHECK);
    if (attrs.DEFAULT_SYMBOL() != null) out.flag(COLUMN_DEF_DEFAULT);
    if (attrs.AUTO_INCREMENT_SYMBOL() != null) out.flag(COLUMN_DEF_AUTOINCREMENT);
  }

  static void collectGColumnAttr(MySQLParser.GcolAttributeContext attrs, Attrs out) {
    if (attrs.notRule() != null) out.flag(COLUMN_DEF_CONS, Constraint.NOT_NULL);
    if (attrs.UNIQUE_SYMBOL() != null) out.flag(COLUMN_DEF_CONS, Constraint.UNIQUE);
    if (attrs.PRIMARY_SYMBOL() != null) out.flag(COLUMN_DEF_CONS, Constraint.PRIMARY);
  }

  Set<String> TIME_TYPE = Set.of(YEAR, DATE, TIME, TIMESTAMP, DATETIME);
  Set<String> FRACTION_TYPES = Set.of(REAL, DOUBLE, FLOAT, DECIMAL, NUMERIC, FIXED);

  static IndexType parseIndexType(MySQLParser.IndexTypeContext indexType) {
    if (indexType == null) return null;

    switch (indexType.algorithm.getText().toLowerCase()) {
      case "rtree":
        return IndexType.RTREE;
      case "hash":
        return IndexType.HASH;
      case "btree":
        return IndexType.BTREE;
      default:
        return null;
    }
  }

  static IndexType parseIndexType(MySQLParser.IndexNameAndTypeContext ctx) {
    if (ctx == null) return null;
    return parseIndexType(ctx.indexType());
  }

  static IndexType parseIndexType(List<MySQLParser.IndexOptionContext> ctx) {
    for (MySQLParser.IndexOptionContext option : ctx) {
      final var indexTypeClause = option.indexTypeClause();
      if (indexTypeClause != null) return parseIndexType(indexTypeClause.indexType());
    }
    return null;
  }

  static KeyDirection parseDirection(MySQLParser.DirectionContext ctx) {
    if (ctx == null) return null;
    if (ctx.ASC_SYMBOL() != null) return ASC;
    else return DESC;
  }

  static String stringifyIndexName(MySQLParser.IndexNameAndTypeContext ctx) {
    if (ctx == null) return null;
    return stringifyIndexName(ctx.indexName());
  }

  static String stringifyIndexName(MySQLParser.IndexNameContext ctx) {
    if (ctx == null) return null;
    return stringifyIdentifier(ctx.identifier());
  }

  static int fieldLength2Int(MySQLParser.FieldLengthContext ctx) {
    final var decimalNum = ctx.DECIMAL_NUMBER();
    final var number = ctx.real_ulonglong_number();

    if (decimalNum != null) {
      return Double.valueOf(decimalNum.getText()).intValue();

    } else if (number != null) {
      final var text = number.getText();
      if (text.startsWith("0x")) return Integer.parseInt(text.substring(2));
      else if (text.startsWith("x'")) return Integer.parseInt(text.substring(2, text.length() - 1));
      else return Integer.parseInt(text);

    } else {
      assert false;
      return -1;
    }
  }

  static SQLDataType parseDataType(MySQLParser.DataTypeContext ctx) {
    final var typeString = ctx.type.getText().toLowerCase();

    final SQLDataCategory category;
    final String name;
    if (typeString.endsWith(INT) || typeString.equals(BIT) || typeString.equals(SERIAL)) {
      category = SQLDataCategory.INTEGRAL;
      name = typeString;

    } else if (FRACTION_TYPES.contains(typeString)) {
      category = SQLDataCategory.FRACTION;
      name = typeString;

    } else if (TIME_TYPE.contains(typeString)) {
      category = SQLDataCategory.TIME;
      name = typeString;

    } else if (typeString.contains("bool")) {
      category = SQLDataCategory.BOOLEAN;
      name = typeString;

    } else if (typeString.contains("blob")) {
      category = SQLDataCategory.BLOB;
      name = typeString;

    } else if (SET.equals(typeString) || ENUM.equals(typeString)) {
      category = SQLDataCategory.ENUM;
      name = typeString;

    } else if (JSON.equals(typeString)) {
      category = SQLDataCategory.JSON;
      name = typeString;

    } else if (typeString.endsWith("char")
        || typeString.endsWith(BINARY)
        || typeString.endsWith(TEXT)
        || typeString.endsWith("varying")) {
      category = SQLDataCategory.STRING;
      if (typeString.contains("char")) name = typeString.contains("var") ? VARCHAR : CHAR;
      else if (typeString.contains("binary"))
        name = typeString.contains("var") ? VARBINARY : BINARY;
      else name = typeString;

    } else {
      category = SQLDataCategory.GEO;
      name = typeString;
    }

    final var fieldLength = ctx.fieldLength();
    final var floatOptions = ctx.floatOptions();
    final var precision = ctx.precision();
    final int w, p;
    if (fieldLength != null) {
      w = fieldLength2Int(fieldLength);
      p = -1;

    } else if (floatOptions != null) {
      final int[] widthAndPrecision = floatOptions2Int(floatOptions);
      assert widthAndPrecision != null;
      w = widthAndPrecision[0];
      p = widthAndPrecision[1];

    } else if (precision != null) {
      final int[] widthAndPrecision = precision2Int(precision);
      w = widthAndPrecision[0];
      p = widthAndPrecision[1];

    } else {
      w = -1;
      p = -1;
    }

    final var fieldOptions = ctx.fieldOptions();
    final boolean unsigned =
        SERIAL.equals(name) || fieldOptions != null && fieldOptions.UNSIGNED_SYMBOL() != null;

    final var stringList = ctx.stringList();
    final List<String> valuesList =
        stringList == null
            ? emptyList()
            : FuncUtils.listMap(MySQLParser.TextStringContext::getText, stringList.textString());

    return new SQLDataType(category, name, w, p, unsigned, valuesList);
  }

  private static int[] precision2Int(MySQLParser.PrecisionContext ctx) {
    final int[] ret = new int[2];
    ret[0] = Integer.parseInt(ctx.INT_NUMBER(1).getText());
    ret[1] = Integer.parseInt(ctx.INT_NUMBER(3).getText());
    return ret;
  }

  private static int[] floatOptions2Int(MySQLParser.FloatOptionsContext ctx) {
    final var fieldLength = ctx.fieldLength();
    final var precision = ctx.precision();

    if (fieldLength != null) {
      return new int[] {-1, fieldLength2Int(fieldLength)};

    } else if (precision != null) {
      return precision2Int(precision);

    } else {
      assert false;
      return null;
    }
  }
}
