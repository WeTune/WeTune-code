package sjtu.ipads.wtune.sqlparser.mysql;

import org.antlr.v4.runtime.Token;
import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.SQLDataType;
import sjtu.ipads.wtune.sqlparser.SQLExpr.IntervalUnit;
import sjtu.ipads.wtune.sqlparser.SQLExpr.LiteralType;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLTableSource;
import sjtu.ipads.wtune.sqlparser.mysql.internal.MySQLLexer;
import sjtu.ipads.wtune.sqlparser.mysql.internal.MySQLParser;

import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static sjtu.ipads.wtune.common.utils.Commons.unquoted;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.SQLDataType.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.KeyDirection.ASC;
import static sjtu.ipads.wtune.sqlparser.SQLNode.KeyDirection.DESC;

public interface MySQLASTHelper {
  static SQLNode newNode(Type type) {
    return new SQLNode(MYSQL, type);
  }

  static String stringifyText(MySQLParser.TextStringContext text) {
    if (text.textStringLiteral() != null) return stringifyText(text.textStringLiteral());
    else if (text.HEX_NUMBER() != null) return text.HEX_NUMBER().getText();
    else if (text.BIN_NUMBER() != null) return text.BIN_NUMBER().getText();
    else {
      assert false;
      return null;
    }
  }

  static String stringifyText(MySQLParser.TextLiteralContext text) {
    return String.join("", listMap(MySQLASTHelper::stringifyText, text.textStringLiteral()));
  }

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

  /** @return string[3] */
  static String[] stringifyIdentifier(MySQLParser.SimpleIdentifierContext id) {
    final String[] triple = new String[3];

    if (id.dotIdentifier(1) != null) {
      triple[0] = stringifyIdentifier(id.identifier());
      triple[1] = stringifyIdentifier(id.dotIdentifier(0));
      triple[2] = stringifyIdentifier(id.dotIdentifier(1));
    } else if (id.dotIdentifier(0) != null) {
      triple[0] = null;
      triple[1] = stringifyIdentifier(id.identifier());
      triple[2] = stringifyIdentifier(id.dotIdentifier(0));
    } else if (id.identifier() != null) {
      triple[0] = null;
      triple[1] = null;
      triple[2] = stringifyIdentifier(id.identifier());
    } else {
      assert false;
      return null;
    }

    return triple;
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
      out.flag(COLUMN_DEF_CONS, ConstraintType.NOT_NULL);
    if (attrs.UNIQUE_SYMBOL() != null) out.flag(COLUMN_DEF_CONS, ConstraintType.UNIQUE);
    if (attrs.PRIMARY_SYMBOL() != null) out.flag(COLUMN_DEF_CONS, ConstraintType.PRIMARY);
    if (attrs.checkConstraint() != null) out.flag(COLUMN_DEF_CONS, ConstraintType.CHECK);
    if (attrs.DEFAULT_SYMBOL() != null) out.flag(COLUMN_DEF_DEFAULT);
    if (attrs.AUTO_INCREMENT_SYMBOL() != null) out.flag(COLUMN_DEF_AUTOINCREMENT);
  }

  static void collectGColumnAttr(MySQLParser.GcolAttributeContext attrs, Attrs out) {
    if (attrs.notRule() != null) out.flag(COLUMN_DEF_CONS, ConstraintType.NOT_NULL);
    if (attrs.UNIQUE_SYMBOL() != null) out.flag(COLUMN_DEF_CONS, ConstraintType.UNIQUE);
    if (attrs.PRIMARY_SYMBOL() != null) out.flag(COLUMN_DEF_CONS, ConstraintType.PRIMARY);
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

  static IntervalUnit parseIntervalUnit(MySQLParser.IntervalContext ctx) {
    return IntervalUnit.valueOf(ctx.getText().toUpperCase());
  }

  static SQLDataType parseDataType(MySQLParser.DataTypeContext ctx) {
    final var typeString = ctx.type.getText().toLowerCase();

    final SQLDataType.Category category;
    final String name;
    if (typeString.endsWith(INT) || typeString.equals(BIT) || typeString.equals(SERIAL)) {
      category = SQLDataType.Category.INTEGRAL;
      name = typeString;

    } else if (FRACTION_TYPES.contains(typeString)) {
      category = SQLDataType.Category.FRACTION;
      name = typeString;

    } else if (TIME_TYPE.contains(typeString)) {
      category = SQLDataType.Category.TIME;
      name = typeString;

    } else if (typeString.contains("bool")) {
      category = SQLDataType.Category.BOOLEAN;
      name = typeString;

    } else if (typeString.contains("blob")) {
      category = SQLDataType.Category.BLOB;
      name = typeString;

    } else if (SET.equals(typeString) || ENUM.equals(typeString)) {
      category = SQLDataType.Category.ENUM;
      name = typeString;

    } else if (JSON.equals(typeString)) {
      category = SQLDataType.Category.JSON;
      name = typeString;

    } else if (typeString.endsWith("char")
        || typeString.endsWith(BINARY)
        || typeString.endsWith(TEXT)
        || typeString.endsWith("varying")) {
      category = SQLDataType.Category.STRING;
      if (typeString.contains("char")) name = typeString.contains("var") ? VARCHAR : CHAR;
      else if (typeString.contains("binary"))
        name = typeString.contains("var") ? VARBINARY : BINARY;
      else name = typeString;

    } else {
      category = SQLDataType.Category.GEO;
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
            : listMap(MySQLParser.TextStringContext::getText, stringList.textString());

    return new SQLDataType(category, name, w, p, unsigned, valuesList);
  }

  static Pair<LiteralType, Number> parseNumericLiteral(Token token) {
    if (token == null) return null;

    final String text = token.getText();
    switch (token.getType()) {
      case MySQLLexer.INT_NUMBER:
        return Pair.of(LiteralType.INTEGER, Integer.parseInt(text));

      case MySQLLexer.LONG_NUMBER:
      case MySQLLexer.ULONGLONG_NUMBER:
        return Pair.of(LiteralType.LONG, Long.parseLong(text));

      case MySQLLexer.DECIMAL_NUMBER:
      case MySQLLexer.FLOAT_NUMBER:
        return Pair.of(LiteralType.FRACTIONAL, Double.parseDouble(text));

      default:
        return null;
    }
  }

  static OLAPOption parseOLAPOption(MySQLParser.OlapOptionContext ctx) {
    if (ctx == null) return null;
    else if (ctx.ROLLUP_SYMBOL() != null) return OLAPOption.WITH_ROLLUP;
    else if (ctx.CUBE_SYMBOL() != null) return OLAPOption.WITH_CUBE;
    else return null;
  }

  static IndexHintType parseIndexHintType(MySQLParser.IndexHintContext ctx) {
    if (ctx.USE_SYMBOL() != null) return IndexHintType.USE;
    else if (ctx.indexHintType().FORCE_SYMBOL() != null) return IndexHintType.FORCE;
    else if (ctx.indexHintType().IGNORE_SYMBOL() != null) return IndexHintType.IGNORE;
    else return null;
  }

  static IndexHintTarget parseIndexHintTarget(MySQLParser.IndexHintClauseContext ctx) {
    if (ctx == null) return null;
    else if (ctx.JOIN_SYMBOL() != null) return IndexHintTarget.JOIN;
    else if (ctx.ORDER_SYMBOL() != null) return IndexHintTarget.ORDER_BY;
    else if (ctx.GROUP_SYMBOL() != null) return IndexHintTarget.GROUP_BY;
    else return null;
  }

  static String parseIndexListElement(MySQLParser.IndexListElementContext ctx) {
    if (ctx.PRIMARY_SYMBOL() != null) return "PRIMARY";
    else if (ctx.identifier() != null) return stringifyIdentifier(ctx.identifier());
    else return null;
  }

  static SQLTableSource.JoinType parseJoinType(MySQLParser.InnerJoinTypeContext ctx) {
    if (ctx == null) return null;
    else if (ctx.CROSS_SYMBOL() != null) return SQLTableSource.JoinType.CROSS_JOIN;
    else if (ctx.INNER_SYMBOL() != null) return SQLTableSource.JoinType.INNER_JOIN;
    else if (ctx.STRAIGHT_JOIN_SYMBOL() != null) return SQLTableSource.JoinType.STRAIGHT_JOIN;
    else return SQLTableSource.JoinType.INNER_JOIN;
  }

  static SQLTableSource.JoinType parseJoinType(MySQLParser.OuterJoinTypeContext ctx) {
    if (ctx == null) return null;
    else if (ctx.LEFT_SYMBOL() != null) return SQLTableSource.JoinType.LEFT_JOIN;
    else if (ctx.RIGHT_SYMBOL() != null) return SQLTableSource.JoinType.RIGHT_JOIN;
    else return null;
  }

  static SQLTableSource.JoinType parseJoinType(MySQLParser.NaturalJoinTypeContext ctx) {
    if (ctx == null) return null;
    else if (ctx.LEFT_SYMBOL() != null) return SQLTableSource.JoinType.NATURAL_LEFT_JOIN;
    else if (ctx.RIGHT_SYMBOL() != null) return SQLTableSource.JoinType.NATURAL_RIGHT_JOIN;
    else return SQLTableSource.JoinType.NATURAL_INNER_JOIN;
  }

  static SQLNode wrapQuerySpec(SQLNode node) {
    if (node.type() == Type.QUERY_SPEC) {
      final SQLNode query = new SQLNode(Type.QUERY);
      query.put(QUERY_BODY, node);
      return query;
    }
    return node;
  }

  private static int[] precision2Int(MySQLParser.PrecisionContext ctx) {
    final int[] ret = new int[2];
    ret[0] = Integer.parseInt(ctx.INT_NUMBER(0).getText());
    ret[1] = Integer.parseInt(ctx.INT_NUMBER(1).getText());
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
