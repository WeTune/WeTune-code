package sjtu.ipads.wtune.sqlparser.pg;

import org.antlr.v4.runtime.tree.TerminalNode;
import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.*;
import sjtu.ipads.wtune.sqlparser.ast.internal.SQLNodeFactory;
import sjtu.ipads.wtune.sqlparser.pg.internal.PGParser;

import java.util.*;
import java.util.stream.Collectors;

import static sjtu.ipads.wtune.common.utils.Commons.assertFalse;
import static sjtu.ipads.wtune.common.utils.Commons.unquoted;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprType.QUERY_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.constants.LiteralType.*;

interface PGASTHelper {
  static String stringifyIdentifier(PGParser.Id_tokenContext ctx) {
    if (ctx == null) return null;
    // the parser rule has quoted the text already
    return ctx.getText();
  }

  static String stringifyIdentifier(PGParser.Identifier_nontypeContext ctx) {
    if (ctx == null) return null;
    if (ctx.tokens_nonreserved() != null || ctx.tokens_reserved_except_function_type() != null)
      return ctx.getText();
    return stringifyIdentifier(ctx.id_token());
  }

  static String stringifyIdentifier(PGParser.IdentifierContext ctx) {
    if (ctx == null) return null;
    if (ctx.tokens_nonreserved() != null || ctx.tokens_nonreserved_except_function_type() != null)
      return ctx.getText();
    return stringifyIdentifier(ctx.id_token());
  }

  static String stringifyIdentifier(PGParser.Col_labelContext ctx) {
    if (ctx == null) return null;
    if (ctx.id_token() != null) return stringifyIdentifier(ctx.id_token());
    else return ctx.getText();
  }

  /** @return String[3] */
  static String[] stringifyIdentifier(PGParser.Schema_qualified_nameContext ctx) {
    if (ctx == null) return null;
    final var identifiers = ctx.identifier();
    final String str0, str1, str2;
    if (identifiers.size() == 3) {
      str0 = stringifyIdentifier(identifiers.get(0));
      str1 = stringifyIdentifier(identifiers.get(1));
      str2 = stringifyIdentifier(identifiers.get(2));

    } else if (identifiers.size() == 2) {
      str0 = null;
      str1 = stringifyIdentifier(identifiers.get(0));
      str2 = stringifyIdentifier(identifiers.get(1));

    } else if (identifiers.size() == 1) {
      str0 = null;
      str1 = null;
      str2 = stringifyIdentifier(identifiers.get(0));

    } else return assertFalse();

    final String[] triple = new String[3];
    triple[0] = str0;
    triple[1] = str1;
    triple[2] = str2;

    return triple;
  }

  static String[] stringifyIdentifier(PGParser.Schema_qualified_name_nontypeContext ctx) {
    if (ctx == null) return null;
    final String[] pair = new String[2];
    if (ctx.schema != null) pair[0] = stringifyIdentifier(ctx.schema);
    pair[1] = stringifyIdentifier(ctx.identifier_nontype());
    return pair;
  }

  static String stringifyText(PGParser.Character_stringContext ctx) {
    if (ctx == null) return null;
    if (ctx.Text_between_Dollar() != null && !ctx.Text_between_Dollar().isEmpty())
      return String.join("", listMap(TerminalNode::getText, ctx.Text_between_Dollar()));
    else if (ctx.Character_String_Literal() != null) return unquoted(ctx.getText(), '\'');
    else return assertFalse();
  }

  static SQLNode tableName(SQLNodeFactory factory, String[] triple) {
    final SQLNode node = factory.newNode(NodeType.TABLE_NAME);
    node.set(TABLE_NAME_SCHEMA, triple[1]);
    node.set(TABLE_NAME_TABLE, triple[2]);

    return node;
  }

  static SQLNode columnName(SQLNodeFactory factory, String[] triple) {
    final SQLNode node = factory.newNode(NodeType.COLUMN_NAME);
    node.set(COLUMN_NAME_SCHEMA, triple[0]);
    node.set(COLUMN_NAME_TABLE, triple[1]);
    node.set(COLUMN_NAME_COLUMN, triple[2]);
    return node;
  }

  static IndexType parseIndexType(String text) {
    if (text == null) return null;
    return switch (text) {
      case "btree" -> IndexType.BTREE;
      case "hash" -> IndexType.HASH;
      case "rtree", "gist" -> IndexType.GIST;
      case "gin" -> IndexType.GIN;
      case "spgist" -> IndexType.SPGIST;
      case "brin" -> IndexType.BRIN;
      default -> null;
    };
  }

  static List<SQLNode> columnNames(SQLNodeFactory factory, PGParser.Names_referencesContext ctx) {
    return ctx.schema_qualified_name().stream()
        .map(PGASTHelper::stringifyIdentifier)
        .map(it -> columnName(factory, it))
        .collect(Collectors.toList());
  }

  static List<SQLNode> keyParts(SQLNodeFactory factory, PGParser.Names_referencesContext ctx) {
    final var idContexts = ctx.schema_qualified_name();
    final List<SQLNode> keyParts = new ArrayList<>(idContexts.size());

    for (var idContext : idContexts) {
      final SQLNode keyPart = factory.newNode(NodeType.KEY_PART);
      keyPart.set(KEY_PART_COLUMN, stringifyIdentifier(idContext)[2]);

      keyParts.add(keyPart);
    }

    return keyParts;
  }

  static SQLDataType parseDataType(PGParser.Data_typeContext ctx) {
    final var predefinedCtx = ctx.predefined_type();
    final String typeString =
        predefinedCtx.type != null
            ? predefinedCtx.type.getText().toLowerCase()
            : stringifyIdentifier(
                    predefinedCtx.schema_qualified_name_nontype().identifier_nontype())
                .toLowerCase();

    final String name;
    final Category category;
    if (typeString.endsWith("int") || typeString.equals(DataTypeName.INTEGER)) {
      category = Category.INTEGRAL;
      name = typeString.equals("int") ? DataTypeName.INTEGER : typeString;

    } else if (typeString.contains("bit")) {
      category = Category.BIT_STRING;
      name =
          (predefinedCtx.VARYING() != null || typeString.contains("var"))
              ? DataTypeName.BIT_VARYING
              : DataTypeName.BIT;

    } else if (typeString.startsWith("int") && !typeString.equals("interval")) {
      category = Category.INTEGRAL;
      switch (typeString) {
        case "int2":
          name = DataTypeName.SMALLINT;
          break;
        case "int4":
          name = DataTypeName.INTEGER;
          break;
        case "int8":
          name = DataTypeName.BIGINT;
          break;
        default:
          return assertFalse();
      }

    } else if (typeString.endsWith("serial")) {
      category = Category.INTEGRAL;
      name = typeString;

    } else if (typeString.startsWith("serial")) {
      category = Category.INTEGRAL;
      switch (typeString) {
        case "serial2":
          name = DataTypeName.SMALLSERIAL;
          break;
        case "serial4":
          name = DataTypeName.SERIAL;
          break;
        case "serial8":
          name = DataTypeName.BIGSERIAL;
          break;
        default:
          return assertFalse();
      }

    } else if (DataTypeName.FRACTION_TYPES.contains(typeString) || "dec".equals(typeString)) {
      category = Category.FRACTION;
      name = "dec".equals(typeString) ? DataTypeName.DECIMAL : typeString;

    } else if (typeString.startsWith("float")) {
      category = Category.FRACTION;
      switch (typeString) {
        case "float4":
          name = DataTypeName.FLOAT;
          break;
        case "float8":
          name = DataTypeName.DOUBLE;
          break;
        default:
          return assertFalse();
      }

    } else if (typeString.equals("text")) {
      category = Category.STRING;
      name = typeString;

    } else if (DataTypeName.TIME_TYPE.contains(typeString)) {
      category = Category.TIME;
      name = predefinedCtx.WITH() != null ? (typeString + "tz") : typeString;

    } else if (typeString.contains("char")) {
      category = Category.STRING;
      name =
          (typeString.contains("var") || predefinedCtx.VARYING() != null)
              ? DataTypeName.VARCHAR
              : DataTypeName.CHAR;

    } else if (typeString.equals("interval")) {
      category = Category.INTERVAL;
      name = DataTypeName.INTERVAL;

    } else if (typeString.contains("bool")) {
      category = Category.BOOLEAN;
      name = DataTypeName.BOOLEAN;

    } else if (typeString.contains("json")) {
      category = Category.JSON;
      name = typeString;

    } else if (DataTypeName.GEOMETRY_TYPES.contains(typeString)) {
      category = Category.GEO;
      name = typeString;

    } else if (DataTypeName.NET_TYPES.contains(typeString)) {
      category = Category.NET;
      name = typeString;

    } else if (DataTypeName.MONEY.equals(typeString)) {
      category = Category.MONETARY;
      name = typeString;

    } else if (DataTypeName.UUID.equals(typeString)) {
      category = Category.UUID;
      name = typeString;

    } else if (DataTypeName.XML.equals(typeString)) {
      category = Category.XML;
      name = typeString;

    } else if (typeString.endsWith("range")) {
      category = Category.RANGE;
      name = typeString;

    } else {
      category = Category.UNCLASSIFIED;
      name = typeString;
    }

    final var typeLength = predefinedCtx.type_length();
    final var precision = predefinedCtx.precision_param();
    final var intervalField = predefinedCtx.interval_field();

    final int w, p;
    if (typeLength != null) {
      w = Integer.parseInt(typeLength.NUMBER_LITERAL().getText());
      p = -1;

    } else if (precision != null) {
      w = Integer.parseInt(precision.precision.getText());
      p = precision.scale == null ? -1 : Integer.parseInt(precision.scale.getText());

    } else {
      w = -1;
      p = -1;
    }

    final String interval = intervalField == null ? null : intervalField.getText();

    final var arrayDimsCtx = ctx.array_type();
    final int[] arrayDims = new int[arrayDimsCtx.size()];
    for (int i = 0; i < arrayDimsCtx.size(); i++) {
      final var arrayDimCtx = arrayDimsCtx.get(i);
      arrayDims[i] =
          arrayDimCtx.NUMBER_LITERAL() == null
              ? 0
              : Integer.parseInt(arrayDimCtx.NUMBER_LITERAL().getText());
    }

    return SQLDataType.make(category, name, w, p)
        .setIntervalField(interval)
        .setDimensions(arrayDims);
  }

  static Number parseNumericLiteral(PGParser.Unsigned_numeric_literalContext ctx) {
    if (ctx.REAL_NUMBER() != null) return Double.parseDouble(ctx.getText());
    else if (ctx.NUMBER_LITERAL() != null) return Long.parseLong(ctx.getText());
    else return assertFalse();
  }

  static Boolean parseTruthValue(PGParser.Truth_valueContext ctx) {
    if (ctx.TRUE() != null || ctx.ON() != null) return true;
    else if (ctx.FALSE() != null) return false;
    else return assertFalse();
  }

  static Object parseUnsignedValue(PGParser.Unsigned_value_specificationContext ctx) {
    if (ctx.unsigned_numeric_literal() != null)
      return parseNumericLiteral(ctx.unsigned_numeric_literal());
    else if (ctx.character_string() != null) return stringifyText(ctx.character_string());
    else if (ctx.truth_value() != null) return parseTruthValue(ctx.truth_value());
    else return assertFalse();
  }

  static SQLNode parseUnsignedLiteral(
      SQLNodeFactory factory, PGParser.Unsigned_value_specificationContext ctx) {
    final Object value = parseUnsignedValue(ctx);
    if (value instanceof Boolean) return factory.literal(BOOL, value);
    else if (value instanceof Double) return factory.literal(FRACTIONAL, value);
    else if (value instanceof Long)
      return (Long) value <= Integer.MAX_VALUE
          ? factory.literal(INTEGER, value)
          : factory.literal(LONG, value);
    else if (value instanceof String) return factory.literal(LiteralType.TEXT, value);
    else return assertFalse();
  }

  static SQLNode parseParam(SQLNodeFactory factory, PGParser.Dollar_numberContext ctx) {
    return factory.paramMarker(Integer.parseInt(ctx.getText().substring(1)));
  }

  static SQLNode buildIndirection(SQLNodeFactory factory, String id, List<SQLNode> indirections) {
    assert indirections.size() > 0;

    final SQLNode _0 = indirections.get(0);

    if (indirections.size() == 1) return buildIndirection1(factory, id, _0);

    final SQLNode _1 = indirections.get(1);
    final SQLNode header = buildIndirection2(factory, id, _0, _1);

    if (indirections.size() == 2) return header;

    assert ExprType.COLUMN_REF.isInstance(header) || ExprType.INDIRECTION.isInstance(header);

    return ExprType.COLUMN_REF.isInstance(header)
        ? factory.indirection(header, indirections.subList(2, indirections.size()))
        : factory.indirection(
            header.get(INDIRECTION_EXPR),
            header.get(INDIRECTION_COMPS).size() == 2
                ? indirections
                : indirections.subList(1, indirections.size()));
  }

  private static SQLNode buildIndirection1(SQLNodeFactory factory, String id, SQLNode indirection) {
    if (!indirection.isFlag(INDIRECTION_COMP_SUBSCRIPT)) {
      final SQLNode indirectionExpr = indirection.get(INDIRECTION_COMP_START);
      if (ExprType.SYMBOL.isInstance(indirectionExpr))
        return factory.columnRef(id, indirectionExpr.get(SYMBOL_TEXT));
      else if (ExprType.WILDCARD.isInstance(indirectionExpr))
        return factory.wildcard(factory.tableName(id));
    }

    return factory.indirection(factory.columnRef(null, id), Collections.singletonList(indirection));
  }

  private static SQLNode buildIndirection2(
      SQLNodeFactory factory, String id, SQLNode _0, SQLNode _1) {
    if (_0.isFlag(INDIRECTION_COMP_SUBSCRIPT))
      return factory.indirection(factory.columnRef(null, id), Arrays.asList(_0, _1));

    final SQLNode expr0 = _0.get(INDIRECTION_COMP_START);
    if (!ExprType.SYMBOL.isInstance(expr0))
      return factory.indirection(factory.columnRef(null, id), Arrays.asList(_0, _1));

    if (_1.isFlag(INDIRECTION_COMP_SUBSCRIPT))
      return factory.indirection(
          buildIndirection1(factory, id, expr0), Collections.singletonList(_1));

    final SQLNode expr1 = _1.get(INDIRECTION_COMP_START);

    if (ExprType.SYMBOL.isInstance(expr1))
      return factory.columnRef(
          id, expr0.get(SYMBOL_TEXT), expr1.get(SYMBOL_TEXT));
    else if (ExprType.WILDCARD.isInstance(expr1))
      return factory.wildcard(factory.tableName(expr0.get(SYMBOL_TEXT)));
    else
      return factory.indirection(
          factory.columnRef(id, expr0.get(SYMBOL_TEXT)), Collections.singletonList(_1));
  }

  static String parseAlias(PGParser.Alias_clauseContext ctx) {
    return stringifyIdentifier(ctx.alias);
  }

  static JoinType parseJoinType(PGParser.From_itemContext ctx) {
    if (ctx == null) return null;
    if (ctx.NATURAL() != null) {
      if (ctx.LEFT() != null) return JoinType.NATURAL_LEFT_JOIN;
      else if (ctx.RIGHT() != null) return JoinType.NATURAL_RIGHT_JOIN;
      else return JoinType.NATURAL_INNER_JOIN;
    }

    if (ctx.CROSS() != null) return JoinType.CROSS_JOIN;
    if (ctx.INNER() != null) return JoinType.INNER_JOIN;
    if (ctx.LEFT() != null) return JoinType.LEFT_JOIN;
    if (ctx.RIGHT() != null) return JoinType.RIGHT_JOIN;
    if (ctx.FULL() != null) return JoinType.FULL_JOIN;

    return JoinType.INNER_JOIN;
  }

  static int typeLength2Int(PGParser.Type_lengthContext ctx) {
    return Integer.parseInt(ctx.NUMBER_LITERAL().getText());
  }

  static SQLNode warpAsQuery(SQLNodeFactory factory, SQLNode node) {
    if (NodeType.QUERY_SPEC.isInstance(node) || NodeType.SET_OP.isInstance(node)) {
      final SQLNode query = factory.newNode(NodeType.QUERY);
      query.set(QUERY_BODY, node);
      return query;
    }
    return node;
  }

  static SQLNode wrapAsQueryExpr(SQLNodeFactory factory, SQLNode node) {
    assert node.nodeType() == NodeType.QUERY;
    final SQLNode exprNode = factory.newNode(QUERY_EXPR);
    exprNode.set(QUERY_EXPR_QUERY, node);
    return exprNode;
  }

  Set<String> KNOWN_AGG_BASIC =
      Set.of(
          "array_agg",
          "avg",
          "bit_and",
          "bit_or",
          "bool_and",
          "bool_or",
          "count",
          "every",
          "json_agg",
          "jsonb_agg",
          "json_object_agg",
          "jsonb_agg_obejct",
          "max",
          "min",
          "string_agg",
          "sum",
          "xmlagg");

  Set<String> KNOWN_AGG_STATISTIC =
      Set.of(
          "corr",
          "covar_pop",
          "covar_samp",
          "regr_avgx",
          "regr_count",
          "regr_intercept",
          "regr_r2",
          "regr_slope",
          "regr_sxx",
          "regr_sxy",
          "regr_syy",
          "stddev",
          "stddev_pop",
          "stddev_samp",
          "variance",
          "var_pop",
          "var_samp");

  static boolean isAggregator(String[] pair) {
    final String schemaName = pair[0];
    final String funcName = pair[1];
    return schemaName == null
        && (KNOWN_AGG_BASIC.contains(funcName) || KNOWN_AGG_STATISTIC.contains(funcName));
  }
}
