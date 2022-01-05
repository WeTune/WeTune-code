package sjtu.ipads.wtune.sql.util;

import sjtu.ipads.wtune.common.field.FieldKey;
import sjtu.ipads.wtune.sql.ast.SqlContext;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.sql.ast.SqlNodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static sjtu.ipads.wtune.common.utils.ArraySupport.linearFind;
import static sjtu.ipads.wtune.sql.ast.SqlKind.Expr;
import static sjtu.ipads.wtune.sql.ast.SqlKind.TableSource;
import static sjtu.ipads.wtune.sql.ast.SqlNodeFields.Expr_Kind;
import static sjtu.ipads.wtune.sql.ast.SqlNodeFields.TableSource_Kind;

public class SqlCopier {
  private int[] tracks;
  private SqlContext toCtx;
  private SqlNode root;

  private int[] destination;

  public SqlCopier track(int... nodeIds) {
    this.tracks = nodeIds;
    return this;
  }

  public SqlCopier to(SqlContext toCtx) {
    this.toCtx = toCtx;
    return this;
  }

  public SqlCopier root(SqlNode root) {
    this.root = root;
    return this;
  }

  public SqlNode go() {
    if (toCtx == null) toCtx = root.context();
    if (tracks != null) destination = new int[tracks.length];

    final SqlNode copied = copy0(root);
    if (tracks != null) System.arraycopy(destination, 0, tracks, 0, destination.length);
    return copied;
  }

  private SqlNode copy0(SqlNode node) {
    final int newNodeId = toCtx.mkNode(node.kind());

    if (destination != null) {
      final int idx = linearFind(tracks, node.nodeId(), 0);
      if (idx >= 0) destination[idx] = newNodeId;
    }

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
        copiedValue = copy0((SqlNode) value);

      } else if (value instanceof SqlNodes) {
        final SqlNodes nodes = (SqlNodes) value;
        final List<SqlNode> newChildren = new ArrayList<>(nodes.size());
        for (SqlNode sqlNode : nodes) newChildren.add(copy0(sqlNode));
        copiedValue = SqlNodes.mk(toCtx, newChildren);

      } else {
        copiedValue = value;
      }

      toCtx.setFieldOf(newNodeId, key, copiedValue);
    }

    return SqlNode.mk(toCtx, newNodeId);
  }
}
