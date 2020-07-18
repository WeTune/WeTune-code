package sjtu.ipads.wtune.stmt.mutator;

import sjtu.ipads.wtune.sqlparser.SQLExpr;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.analyzer.ColumnRefCollector;
import sjtu.ipads.wtune.stmt.attrs.*;
import sjtu.ipads.wtune.stmt.resolver.ColumnResolver;
import sjtu.ipads.wtune.stmt.resolver.Resolver;
import sjtu.ipads.wtune.stmt.schema.Column;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.*;
import java.util.function.Predicate;

import static sjtu.ipads.wtune.common.utils.FuncUtils.tautology;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.exprKind;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.sqlparser.SQLTableSource.DERIVED_SUBQUERY;
import static sjtu.ipads.wtune.stmt.attrs.SelectItem.selectInputColumn;
import static sjtu.ipads.wtune.stmt.attrs.SelectItem.selectOutputColumn;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_COLUMN_REF;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

public class SelectItemNormalizer implements Mutator, SQLVisitor {
  @Override
  public boolean enterQuery(SQLNode query) {
    return canModify(query);
  }

  @Override
  public void leaveQuery(SQLNode query) {
    if (canModify(query)) modifySelectItem(query);
  }

  private boolean canModify(SQLNode query) {
    final SQLNode parent = query.parent();
    // ignore UNION
    return (parent == null || parent.type() == SQLNode.Type.TABLE_SOURCE)
        && (query.get(QUERY_BODY).type() == SQLNode.Type.QUERY_SPEC);
  }

  private void modifySelectItem(SQLNode query) {
    final QueryScope scope = query.get(RESOLVED_QUERY_SCOPE);
    assert scope instanceof SimpleQueryScope;

    final SQLNode specNode = scope.specNode();
    if (scope.parent() == null) {
      if (!containsAggregate(specNode)) {
        scope.selectItems().clear();
        specNode.get(QUERY_SPEC_SELECT_ITEMS).clear();

        addSelectItems(scope, collectMandatory(scope));
        addSelectItems(scope, selectPrimary(scope));
      }

    } else {
      if (containsAggregate(specNode)) {
        markPrimary(scope);

      } else {
        addSelectItems(scope, selectPrimary(scope));
      }
    }
  }

  private List<SelectItem> selectPrimary(QueryScope scope) {
    final Collection<TableSource> tableSources = scope.tableSources().values();
    final List<SelectItem> items = new ArrayList<>(tableSources.size() * 2);

    for (TableSource tableSource : tableSources)
      if (tableSource.isDerived()) {
        selectOutputColumns(tableSource, items);

      } else {
        if (!(selectInputColumns(tableSource, Column::primaryKeyPart, items)
            || selectInputColumns(tableSource, Column::uniquePart, items)
            || selectInputColumns(tableSource, tautology(), items)))
          throw new StmtException(); // should never reach
      }

    return items;
  }

  private static boolean containsAggregate(SQLNode spec) {
    if (spec.isFlagged(QUERY_SPEC_DISTINCT)
        || spec.get(QUERY_SPEC_DISTINCT_ON) != null
        || spec.get(QUERY_SPEC_GROUP_BY) != null) return true;
    for (SQLNode node : spec.get(QUERY_SPEC_SELECT_ITEMS))
      if (exprKind(node.get(SELECT_ITEM_EXPR)) == SQLExpr.Kind.AGGREGATE) return true;
    return false;
  }

  private static String genAlias(int ordinal) {
    return "_primary_" + ordinal;
  }

  private static boolean selectInputColumns(
      TableSource source, Predicate<Column> filter, List<SelectItem> dest) {
    assert !source.isDerived() && dest != null;

    boolean selected = false;
    for (Column column : source.table().columns())
      if (filter.test(column)) {
        selected = true;
        final SelectItem item = selectInputColumn(source, column, genAlias(dest.size()));
        item.setPrimary(true);

        dest.add(item);
      }
    return selected;
  }

  private static void selectOutputColumns(TableSource source, List<SelectItem> dest) {
    assert source.isDerived();
    for (SelectItem subItem :
        source.node().get(DERIVED_SUBQUERY).get(RESOLVED_QUERY_SCOPE).selectItems()) {
      if (subItem.isPrimary()) {
        final SelectItem item = selectOutputColumn(source, subItem, genAlias(dest.size()));
        item.setPrimary(true);

        dest.add(item);
      }
    }
  }

  private static void markPrimary(QueryScope scope) {
    for (SelectItem item : scope.selectItems()) {
      final ColumnRef cRef = item.expr().get(RESOLVED_COLUMN_REF);
      if (cRef == null) continue;

      if (cRef.refItem() != null) {
        if (cRef.refItem().isPrimary()) item.setPrimary(true);

      } else {
        final Column column = cRef.resolveAsColumn();
        if (column.primaryKeyPart() || column.uniquePart()) item.setPrimary(true);
      }
    }
  }

  private static Set<SelectItem> collectMandatory(QueryScope scope) {
    final Set<SelectItem> ret = new HashSet<>();
    collectRefItems(scope.specNode().get(QUERY_SPEC_GROUP_BY), ret);
    collectRefItems(scope.queryNode().get(QUERY_ORDER_BY), ret);

    return ret;
  }

  private static void collectRefItems(List<SQLNode> nodes, Collection<SelectItem> dest) {
    if (nodes == null) return;

    for (SQLNode node : nodes)
      for (SQLNode refNode : ColumnRefCollector.collect(node)) {
        final ColumnRef cRef = refNode.get(RESOLVED_COLUMN_REF);
        if (cRef.refItem() != null) dest.add(cRef.refItem());
      }
  }

  private static void addSelectItems(QueryScope scope, Collection<SelectItem> items) {
    final SQLNode spec = scope.specNode();
    for (SelectItem item : items) {
      scope.addSelectItem(item);
      spec.get(QUERY_SPEC_SELECT_ITEMS).add(item.node());
    }
  }

  @Override
  public void mutate(Statement stmt) {
    final SQLNode parsed = stmt.parsed();
    parsed.accept(new SelectItemNormalizer());
    parsed.relinkAll();
    parsed.setDbTypeRec(parsed.dbType());
  }

  private static final Set<Class<? extends Resolver>> DEPENDENCIES = Set.of(ColumnResolver.class);

  @Override
  public Set<Class<? extends Resolver>> dependsOnResolver() {
    return DEPENDENCIES;
  }
}
