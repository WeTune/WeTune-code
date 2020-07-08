package sjtu.ipads.wtune.systhesis.predicate;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.SQLDataType;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.analyzer.ColumnRefCollector;
import sjtu.ipads.wtune.stmt.analyzer.NodeFinder;
import sjtu.ipads.wtune.stmt.attrs.BoolExpr;
import sjtu.ipads.wtune.stmt.attrs.ColumnRef;
import sjtu.ipads.wtune.stmt.schema.Column;
import sjtu.ipads.wtune.systhesis.operators.ReplacePredicate;

import java.util.List;

import static sjtu.ipads.wtune.stmt.analyzer.QueryCollector.hasSubquery;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.BOOL_EXPR;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_COLUMN_REF;

public class DisplacePredicate implements PredicateMutator {
  private final SQLNode original;
  private final SQLNode replacement;

  public static final Attrs.Key<Boolean> DISPLACED =
      Attrs.key("synthesis.predicate.displace.marker", Boolean.class);

  public DisplacePredicate(SQLNode original, SQLNode replacement) {
    this.original = original;
    this.replacement = replacement;
  }

  public static boolean canDisplace(SQLNode original, SQLNode replacement) {
    if (original == null || replacement == null) return false;
    if (original.get(DISPLACED) != null) return false;

    final BoolExpr boolExpr0 = original.get(BOOL_EXPR);
    final BoolExpr boolExpr1 = replacement.get(BOOL_EXPR);
    // 1. both are boolean primitive
    if (boolExpr0 == null || boolExpr1 == null) return false;
    if (!boolExpr0.isPrimitive() || !boolExpr1.isPrimitive()) return false;

    // 2. neither contains subquery
    // complex logic is needed to handle subquery, we just omit it for now
    if (hasSubquery(original) || hasSubquery(replacement)) return false;

    final List<SQLNode> oriColumns = ColumnRefCollector.collect(original);
    final List<SQLNode> repColumns = ColumnRefCollector.collect(replacement);

    // 3. they contain columns of the same number
    if (oriColumns.size() != repColumns.size()) return false;

    // 4. these columns has compatible types
    for (int i = 0; i < oriColumns.size(); i++) {
      final ColumnRef oriRef = oriColumns.get(i).get(RESOLVED_COLUMN_REF);
      final ColumnRef repRef = repColumns.get(i).get(RESOLVED_COLUMN_REF);
      assert oriRef != null && repRef != null;

      final Column oriColumn = oriRef.resolveAsColumn();
      final Column repColumn = repRef.resolveAsColumn();
      if (oriColumn == null || repColumn == null) return false;

      final SQLDataType oriType = oriColumn.dataType();
      final SQLDataType repType = repColumn.dataType();
      if (!isCompatibleType(oriType, repType)) return false;
    }

    return true;
  }

  private static boolean isCompatibleType(SQLDataType type0, SQLDataType type1) {
    return type0.category() == type1.category() && type0.isArray() == type1.isArray();
  }

  @Override
  public SQLNode target() {
    return original;
  }

  @Override
  public SQLNode reference() {
    return replacement;
  }

  @Override
  public boolean isValid(SQLNode root) {
    return NodeFinder.find(root, original) != null;
  }

  @Override
  public SQLNode modifyAST(SQLNode root) {
    final SQLNode target = NodeFinder.find(root, this.original);
    ReplacePredicate.build(target, this.replacement.copy()).apply(root);
    target.put(DISPLACED, true);
    return root;
  }
}
