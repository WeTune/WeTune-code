package sjtu.ipads.wtune.sqlparser;

import org.antlr.v4.runtime.misc.ParseCancellationException;
import sjtu.ipads.wtune.common.field.FieldKey;
import sjtu.ipads.wtune.common.tree.LabeledTreeFields;
import sjtu.ipads.wtune.sqlparser.ast1.SqlContext;
import sjtu.ipads.wtune.sqlparser.ast1.SqlKind;
import sjtu.ipads.wtune.sqlparser.ast1.SqlNode;
import sjtu.ipads.wtune.sqlparser.ast1.SqlNodes;
import sjtu.ipads.wtune.sqlparser.parser.AstParser;
import sjtu.ipads.wtune.sqlparser.schema.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static sjtu.ipads.wtune.sqlparser.ast1.SqlKind.Expr;
import static sjtu.ipads.wtune.sqlparser.ast1.SqlKind.TableSource;
import static sjtu.ipads.wtune.sqlparser.ast1.SqlNodeFields.Expr_Kind;
import static sjtu.ipads.wtune.sqlparser.ast1.SqlNodeFields.TableSource_Kind;

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

  public static SqlNode copyAst(SqlNode node, SqlContext toCtx) {
    if (toCtx == null) toCtx = node.context();
    final int newNodeId = toCtx.mkNode(node.kind());

    if (TableSource.isInstance(node)) {
      toCtx.setFieldOf(newNodeId, TableSource_Kind, node.$(TableSource_Kind));
    }
    if (Expr.isInstance(node)) {
      toCtx.setFieldOf(newNodeId, Expr_Kind, node.$(Expr_Kind));
    }

    for (Map.Entry<FieldKey<?>, Object> pair : node.entrySet()) {
      final FieldKey key = pair.getKey();
      final Object value = pair.getValue();
      final Object copiedValue;
      if (value instanceof SqlNode) {
        copiedValue = copyAst((SqlNode) value, toCtx);

      } else if (value instanceof SqlNodes) {
        final SqlNodes nodes = (SqlNodes) value;
        final List<SqlNode> newChildren = new ArrayList<>(nodes.size());
        for (SqlNode sqlNode : nodes) newChildren.add(copyAst(sqlNode, toCtx));
        copiedValue = SqlNodes.mk(toCtx, newChildren);

      } else {
        copiedValue = value;
      }

      toCtx.setFieldOf(newNodeId, key, copiedValue);
    }

    return SqlNode.mk(toCtx, newNodeId);
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
}
