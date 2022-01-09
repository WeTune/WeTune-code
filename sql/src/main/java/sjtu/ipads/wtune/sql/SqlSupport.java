package sjtu.ipads.wtune.sql;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import sjtu.ipads.wtune.common.field.FieldKey;
import sjtu.ipads.wtune.common.tree.LabeledTreeFields;
import sjtu.ipads.wtune.sql.ast.*;
import sjtu.ipads.wtune.sql.ast.constants.*;
import sjtu.ipads.wtune.sql.parser.AstParser;
import sjtu.ipads.wtune.sql.schema.Schema;
import sjtu.ipads.wtune.sql.util.SqlCopier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static sjtu.ipads.wtune.common.tree.TreeSupport.nodeEquals;
import static sjtu.ipads.wtune.common.utils.Commons.unquoted;
import static sjtu.ipads.wtune.sql.ast.ExprFields.*;
import static sjtu.ipads.wtune.sql.ast.ExprKind.*;
import static sjtu.ipads.wtune.sql.ast.SqlKind.*;
import static sjtu.ipads.wtune.sql.ast.SqlNodeFields.*;
import static sjtu.ipads.wtune.sql.ast.TableSourceFields.*;
import static sjtu.ipads.wtune.sql.ast.TableSourceKind.*;
import static sjtu.ipads.wtune.sql.ast.constants.BinaryOpKind.*;

public abstract class SqlSupport {
  private static boolean PARSING_ERROR_MUTED = false;

  private SqlSupport() {}

  public static void muteParsingError() {
    PARSING_ERROR_MUTED = true;
  }

  public static SqlNode parseSql(String dbType, String sql) {
    try {
      return AstParser.ofDb(dbType).parse(sql);
    } catch (ParseCancellationException ex) {
      if (!PARSING_ERROR_MUTED) System.err.println(ex.getMessage());
      return null;
    }
  }

  public static Schema parseSchema(String dbType, String schemaDef) {
    return Schema.parse(dbType, schemaDef);
  }

  public static String quoted(String dbType, String name) {
    if ("mysql".equals(dbType)) return '`' + name + '`';
    else if ("postgresql".equals(dbType)) return '"' + name + '"';
    else if ("mssql".equals(dbType)) return '[' + name + ']';
    else throw new IllegalArgumentException("unknown db type: " + dbType);
  }

  public static List<String> splitSql(String str) {
    final List<String> list = new ArrayList<>(str.length() / 100);

    boolean inSql = false;
    boolean inQuote = false;
    boolean inComment = false;
    boolean escape = false;
    boolean hyphen = false;
    int start = 0;

    for (int i = 0; i < str.length(); i++) {
      final char c = str.charAt(i);

      if (inComment) {
        assert !inSql;
        if (c == '\n' || c == '\r') inComment = false;
        continue;
      }

      if (!inSql) {
        if (Character.isSpaceChar(c) || c == '\n' || c == '\r') continue;
        else {
          inSql = true;
          start = i;
        }
      }

      if (c != '-') hyphen = false;

      switch (c) {
        case '\\':
          escape = true;
          continue;

        case '`':
        case '"':
        case '\'':
          if (!escape) inQuote = !inQuote;
          break;

        case '-':
          if (!inQuote) {
            if (!hyphen) hyphen = true;
            else {
              if (start < i - 1) list.add(str.substring(start, i - 1));
              inComment = true;
              inSql = false;
              hyphen = false;
            }
          }
          continue;

        case ';':
          if (!inQuote) list.add(str.substring(start, i));
          inSql = false;
          break;
      }
      escape = false;
    }

    if (inSql) list.add(str.substring(start));

    return list;
  }

  public static String simpleName(String name) {
    return name == null ? null : unquoted(unquoted(name, '"'), '`').toLowerCase();
  }

  public static int[] idsOf(List<SqlNode> nodes) {
    final int[] ids = new int[nodes.size()];
    for (int i = 0, bound = nodes.size(); i < bound; i++) {
      ids[i] = nodes.get(i).nodeId();
    }
    return ids;
  }

  public static SqlCopier copyAst(SqlNode node) {
    return new SqlCopier().root(node);
  }

  public static SqlNode copyAst(SqlNode node, SqlContext toCtx) {
    return new SqlCopier().root(node).to(toCtx).go();
  }

  public static String dumpAst(SqlNode root) {
    return dumpAst(root, new StringBuilder(), 0).toString();
  }

  public static StringBuilder dumpAst(SqlNode root, StringBuilder builder) {
    return dumpAst(root, builder, 0);
  }

  private static StringBuilder dumpAst(SqlNode root, StringBuilder builder, int level) {
    if (root == null) return builder;

    final SqlContext context = root.context();
    final LabeledTreeFields<SqlKind> fields = context.fieldsOf(root.nodeId());

    builder.append(" ".repeat(level)).append(root.nodeId()).append(' ').append(root.kind());
    builder.append('\n');

    if (fields == null) return builder;

    for (Map.Entry<FieldKey<?>, Object> pair : fields.entrySet()) {
      final FieldKey<?> key = pair.getKey();
      final Object value = pair.getValue();
      if (value instanceof SqlNode || value instanceof SqlNodes) continue;
      builder.append(" ".repeat(level + 1)).append(key).append('=').append(value).append('\n');
    }

    for (Map.Entry<FieldKey<?>, Object> pair : fields.entrySet()) {
      final FieldKey<?> key = pair.getKey();
      final Object value = pair.getValue();
      if (value instanceof SqlNode) {
        builder.append(" ".repeat(level + 1)).append(key).append('=').append('\n');
        dumpAst((SqlNode) value, builder, level + 1);
      }
    }

    for (Map.Entry<FieldKey<?>, Object> pair : fields.entrySet()) {
      final FieldKey<?> key = pair.getKey();
      final Object value = pair.getValue();
      if (value instanceof SqlNodes) {
        builder.append(" ".repeat(level + 1)).append(key).append('=').append('\n');
        for (SqlNode child : ((SqlNodes) value)) {
          dumpAst(child, builder, level + 1);
        }
      }
    }

    return builder;
  }

  public static SqlNode mkName2(SqlContext ctx, String piece0, String piece1) {
    final SqlNode node = SqlNode.mk(ctx, Name2);
    if (piece0 != null) node.$(Name2_0, piece0);
    node.$(Name2_1, piece1);
    return node;
  }

  public static SqlNode mkTableName(SqlContext ctx, String tableName) {
    final SqlNode nameNode = SqlNode.mk(ctx, TableName);
    nameNode.$(TableName_Table, tableName);
    return nameNode;
  }

  public static SqlNode mkColName(SqlContext ctx, String qualification, String name) {
    final SqlNode colName = SqlNode.mk(ctx, ColName);
    colName.$(ColName_Table, qualification);
    colName.$(ColName_Col, name);
    return colName;
  }

  public static SqlNode mkColRef(SqlContext ctx, String qualification, String name) {
    final SqlNode colName = mkColName(ctx, qualification, name);
    final SqlNode colRef = SqlNode.mk(ctx, ColRef);
    colRef.$(ColRef_ColName, colName);
    return colRef;
  }

  public static SqlNode mkWildcard(SqlContext ctx, String tableName) {
    final SqlNode wildcard = SqlNode.mk(ctx, Wildcard);
    wildcard.$(Wildcard_Table, mkTableName(ctx, tableName));
    return wildcard;
  }

  public static SqlNode mkUnary(SqlContext ctx, UnaryOpKind op, SqlNode operand) {
    expect(operand, Expr);
    final SqlNode unary = SqlNode.mk(ctx, Unary);
    unary.$(Unary_Op, op);
    unary.$(Unary_Expr, operand);
    return unary;
  }

  public static SqlNode mkBinary(SqlContext ctx, BinaryOpKind op, SqlNode lhs, SqlNode rhs) {
    expect(lhs, Expr);
    expect(rhs, Expr);

    final SqlNode binary = SqlNode.mk(ctx, Binary);
    binary.$(Binary_Op, op);
    binary.$(Binary_Left, lhs);
    binary.$(Binary_Right, rhs);
    return binary;
  }

  public static SqlNode mkFuncCall(SqlContext ctx, String funcName, List<SqlNode> args) {
    final SqlNode funcCall = SqlNode.mk(ctx, FuncCall);
    final SqlNodes argPack = SqlNodes.mk(ctx, args);
    funcCall.$(FuncCall_Name, mkName2(ctx, null, funcName));
    funcCall.$(FuncCall_Args, argPack);
    return funcCall;
  }

  public static SqlNode mkSelectItem(SqlContext ctx, SqlNode expr, String alias) {
    expect(expr, Expr);

    final SqlNode selectItem = SqlNode.mk(ctx, SelectItem);
    selectItem.$(SelectItem_Expr, expr);
    if (alias != null) selectItem.$(SelectItem_Alias, alias);
    return selectItem;
  }

  public static SqlNode mkLiteral(SqlContext ctx, LiteralKind kind, Object value) {
    final SqlNode literal = SqlNode.mk(ctx, Literal);
    literal.$(Literal_Kind, kind);
    literal.$(Literal_Value, value);
    return literal;
  }

  public static SqlNode mkQueryExpr(SqlContext ctx, SqlNode query) {
    expect(query, Query);
    final SqlNode expr = SqlNode.mk(ctx, QueryExpr);
    expr.$(QueryExpr_Query, query);
    return expr;
  }

  public static SqlNode mkConjunction(SqlContext ctx, Iterable<SqlNode> filters) {
    SqlNode expr = null;
    for (SqlNode filter : filters) {
      expect(filter, Expr);
      if (expr == null) expr = filter;
      else expr = mkBinary(ctx, AND, expr, filter);
    }
    return expr;
  }

  public static SqlNode mkSimpleSource(SqlContext ctx, String tableName, String alias) {
    final SqlNode nameNode = mkTableName(ctx, tableName);
    final SqlNode tableSourceNode = SqlNode.mk(ctx, SimpleSource);
    tableSourceNode.$(Simple_Table, nameNode);
    tableSourceNode.$(Simple_Alias, alias);
    return tableSourceNode;
  }

  public static SqlNode mkJoinSource(
      SqlContext ctx, SqlNode lhs, SqlNode rhs, SqlNode cond, JoinKind kind) {
    expect(lhs, TableSource);
    expect(rhs, TableSource);

    final SqlNode joinNode = SqlNode.mk(ctx, JoinedSource);
    joinNode.$(Joined_Left, lhs);
    joinNode.$(Joined_Right, rhs);
    joinNode.$(Joined_On, cond);
    joinNode.$(Joined_Kind, kind);
    return joinNode;
  }

  public static SqlNode mkDerivedSource(SqlContext ctx, SqlNode query, String alias) {
    expect(query, Query);
    final SqlNode sourceNode = SqlNode.mk(ctx, DerivedSource);
    sourceNode.$(Derived_Subquery, query);
    sourceNode.$(Derived_Alias, alias);
    return sourceNode;
  }

  public static SqlNode mkSetOp(SqlContext ctx, SqlNode lhs, SqlNode rhs, SetOpKind kind) {
    expect(lhs, Query);
    expect(rhs, Query);

    final SqlNode setOpNode = SqlNode.mk(ctx, SetOp);
    setOpNode.$(SetOp_Left, lhs);
    setOpNode.$(SetOp_Right, rhs);
    setOpNode.$(SetOp_Kind, kind);
    return setOpNode;
  }

  public static SqlNode mkQuery(SqlContext ctx, SqlNode body) {
    if (!QuerySpec.isInstance(body) && !SetOp.isInstance(body))
      throw new IllegalArgumentException("invalid query body: " + body.kind());

    final SqlNode q = SqlNode.mk(ctx, Query);
    q.$(Query_Body, body);
    return q;
  }

  public static SqlNode mkAggregate(SqlContext ctx, List<SqlNode> args, String aggFuncName) {
    final SqlNodes argPack = SqlNodes.mk(ctx, args);
    final SqlNode aggregate = SqlNode.mk(ctx, Aggregate);
    aggregate.$(Aggregate_Name, aggFuncName);
    aggregate.$(Aggregate_Args, argPack);
    return aggregate;
  }

  public static boolean isColRefEq(SqlNode ast) {
    return Binary.isInstance(ast)
        && EQUAL == ast.$(Binary_Op)
        && ColRef.isInstance(ast.$(Binary_Left))
        && ColRef.isInstance(ast.$(Binary_Right));
  }

  /** Check if the ast is of the form "col0 = const_value", where const_value is not "NULL" */
  public static boolean isEquiConstPredicate(SqlNode ast) {
    final SqlNode lhs = ast.$(Binary_Left);
    final SqlNode rhs = ast.$(Binary_Right);
    final BinaryOpKind op = ast.$(Binary_Op);

    final SqlNode literal;
    if (ColRef.isInstance(lhs) && Literal.isInstance(rhs)) literal = rhs;
    else if (ColRef.isInstance(rhs) && Literal.isInstance(lhs)) literal = lhs;
    else return false;

    return (op == IS || op == EQUAL || op == NULL_SAFE_EQUAL)
        && literal.$(Literal_Kind) != LiteralKind.NULL;
  }

  /** Check if the ast is of the form "col0 = col1 [AND col2 = col3 [AND ...]]" */
  public static boolean isEquiJoinPredicate(SqlNode ast) {
    if (!Binary.isInstance(ast)) return false;
    final BinaryOpKind op = ast.$(Binary_Op);
    if (op == BinaryOpKind.AND) {
      return isEquiJoinPredicate(ast.$(Binary_Left)) && isEquiJoinPredicate(ast.$(Binary_Right));
    } else {
      return isColRefEq(ast);
    }
  }

  public static boolean isPrimitivePredicate(SqlNode ast) {
    return Expr.isInstance(ast)
        && (!Binary.isInstance(ast) || !ast.$(Binary_Op).isLogic())
        && (!Unary.isInstance(ast) || !ast.$(Unary_Op).isLogic());
  }

  public static SqlNode getAnotherSide(SqlNode binaryExpr, SqlNode thisSide) {
    if (!Binary.isInstance(binaryExpr)) return null;
    final SqlNode lhs = binaryExpr.$(Binary_Left);
    final SqlNode rhs = binaryExpr.$(Binary_Right);
    if (nodeEquals(lhs, thisSide)) return rhs;
    if (nodeEquals(rhs, thisSide)) return lhs;
    return null;
  }

  public static String selectItemNameOf(SqlNode selectItem) {
    expect(selectItem, SelectItem);
    final String alias = selectItem.$(SelectItem_Alias);
    if (alias != null) return alias;

    final SqlNode exprAst = selectItem.$(SelectItem_Expr);
    if (ColRef.isInstance(exprAst)) return exprAst.$(ColRef_ColName).$(ColName_Col);
    return null;
  }

  public static List<SqlNode> linearizeConjunction(SqlNode expr) {
    expect(expr, Expr);
    return linearizeConjunction(expr, new ArrayList<>(5));
  }

  public static List<SqlNode> linearizeConjunction(SqlNode expr, List<SqlNode> terms) {
    final BinaryOpKind op = expr.$(Binary_Op);
    if (op != AND) terms.add(expr);
    else {
      linearizeConjunction(expr.$(Binary_Left), terms);
      linearizeConjunction(expr.$(Binary_Right), terms);
    }
    return terms;
  }

  private static void expect(SqlNode node, TableSourceKind expected) {
    if (!expected.isInstance(node))
      throw new IllegalArgumentException(
          "expect " + expected + ", but get " + node.$(TableSource_Kind));
  }

  private static void expect(SqlNode node, ExprKind expected) {
    if (!expected.isInstance(node))
      throw new IllegalArgumentException("expect " + expected + ", but get " + node.$(Expr_Kind));
  }

  private static void expect(SqlNode node, SqlKind expected) {
    if (!expected.isInstance(node))
      throw new IllegalArgumentException("expect " + expected + ", but get " + node.kind());
  }
}
