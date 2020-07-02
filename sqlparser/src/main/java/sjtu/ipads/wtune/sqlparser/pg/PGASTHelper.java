package sjtu.ipads.wtune.sqlparser.pg;

import sjtu.ipads.wtune.sqlparser.SQLDataType;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.pg.internal.PGParser;

import static sjtu.ipads.wtune.common.utils.Commons.assertFalse;
import static sjtu.ipads.wtune.sqlparser.SQLDataType.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.TABLE_NAME_SCHEMA;
import static sjtu.ipads.wtune.sqlparser.SQLNode.TABLE_NAME_TABLE;

interface PGASTHelper {
  static String stringifyIdentifier(PGParser.Id_tokenContext ctx) {
    // the parser rule has quoted the text already
    return ctx.getText();
  }

  static String stringifyIdentifier(PGParser.Identifier_nontypeContext ctx) {
    if (ctx.tokens_nonreserved() != null || ctx.tokens_reserved_except_function_type() != null)
      return ctx.getText();
    return stringifyIdentifier(ctx.id_token());
  }

  static String stringifyIdentifier(PGParser.IdentifierContext ctx) {
    if (ctx.tokens_nonreserved() != null || ctx.tokens_nonreserved_except_function_type() != null)
      return ctx.getText();
    return stringifyIdentifier(ctx.id_token());
  }

  static SQLNode tableName(PGParser.Schema_qualified_nameContext ctx) {
    final var identifiers = ctx.identifier();
    final String schema, table;
    if (identifiers.size() == 3) {
      schema = stringifyIdentifier(identifiers.get(1));
      table = stringifyIdentifier(identifiers.get(2));
    } else if (identifiers.size() == 2) {
      schema = stringifyIdentifier(identifiers.get(0));
      table = stringifyIdentifier(identifiers.get(1));
    } else if (identifiers.size() == 1) {
      schema = null;
      table = stringifyIdentifier(identifiers.get(0));
    } else return assertFalse();

    final SQLNode node = new SQLNode(SQLNode.Type.TABLE_NAME);
    node.put(TABLE_NAME_SCHEMA, schema);
    node.put(TABLE_NAME_TABLE, table);

    return node;
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
    final SQLDataType.Category category;
    if (typeString.endsWith("int") || typeString.equals(INT)) {
      category = SQLDataType.Category.INTEGRAL;
      name = typeString.equals("int") ? INT : typeString;

    } else if (typeString.contains("bit")) {
      category = Category.BIT_STRING;
      name = (predefinedCtx.VARYING() != null || typeString.contains("var")) ? BIT_VARYING : BIT;

    } else if (typeString.startsWith("int") && !typeString.equals("interval")) {
      category = SQLDataType.Category.INTEGRAL;
      switch (typeString) {
        case "int2":
          name = SMALLINT;
          break;
        case "int4":
          name = INT;
          break;
        case "int8":
          name = BIGINT;
          break;
        default:
          return assertFalse();
      }

    } else if (typeString.endsWith("serial")) {
      category = Category.INTEGRAL;
      name = typeString;

    } else if (typeString.startsWith("serial")) {
      category = SQLDataType.Category.INTEGRAL;
      switch (typeString) {
        case "serial2":
          name = SMALLSERIAL;
          break;
        case "serial4":
          name = SERIAL;
          break;
        case "serial8":
          name = BIGSERIAL;
          break;
        default:
          return assertFalse();
      }

    } else if (FRACTION_TYPES.contains(typeString) || "dec".equals(typeString)) {
      category = Category.FRACTION;
      name = "dec".equals(typeString) ? DECIMAL : typeString;

    } else if (typeString.startsWith("float")) {
      category = Category.FRACTION;
      switch (typeString) {
        case "float4":
          name = FLOAT;
          break;
        case "float8":
          name = DOUBLE;
          break;
        default:
          return assertFalse();
      }

    } else if (typeString.equals("text")) {
      category = Category.STRING;
      name = typeString;

    } else if (TIME_TYPE.contains(typeString)) {
      category = Category.TIME;
      name = predefinedCtx.WITH() != null ? (typeString + "tz") : typeString;

    } else if (typeString.contains("char")) {
      category = Category.STRING;
      name = (typeString.contains("var") || predefinedCtx.VARYING() != null) ? VARCHAR : CHAR;

    } else if (typeString.equals("interval")) {
      category = Category.INTERVAL;
      name = INTERVAL;

    } else if (typeString.contains("bool")) {
      category = Category.BOOLEAN;
      name = BOOLEAN;

    } else if (typeString.contains("json")) {
      category = Category.JSON;
      name = typeString;

    } else if (GEOMETRY_TYPES.contains(typeString)) {
      category = Category.GEO;
      name = typeString;

    } else if (NET_TYPES.contains(typeString)) {
      category = Category.NET;
      name = typeString;

    } else if (MONEY.equals(typeString)) {
      category = Category.MONETARY;
      name = typeString;

    } else if (UUID.equals(typeString)) {
      category = Category.UUID;
      name = typeString;

    } else if (XML.equals(typeString)) {
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

    return new SQLDataType(category, name, w, p)
        .setIntervalField(interval)
        .setDimensions(arrayDims);
  }
}
