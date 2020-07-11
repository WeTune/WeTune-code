package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;
import sjtu.ipads.wtune.stmt.attrs.SelectItem;
import sjtu.ipads.wtune.stmt.attrs.TableSource;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.System.Logger.Level.WARNING;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;
import static sjtu.ipads.wtune.stmt.utils.StmtHelper.log;

public class SelectionResolver implements Resolver, SQLVisitor {
  private static final System.Logger LOG = System.getLogger("Stmt.Resolver.Selection");

  private Statement stmt;

  @Override
  public void leaveQuerySpec(SQLNode querySpec) {
    final QueryScope scope = querySpec.get(RESOLVED_QUERY_SCOPE);

    final List<SQLNode> selectItems = querySpec.get(QUERY_SPEC_SELECT_ITEMS);
    final List<SQLNode> modifiedSelectItems = new ArrayList<>(selectItems.size());

    for (SQLNode itemNode : selectItems) {
      final SQLNode expr = itemNode.get(SELECT_ITEM_EXPR);
      if (exprKind(expr) == Kind.WILDCARD) {
        final List<SQLNode> items = fromWildcard(expr);
        modifiedSelectItems.addAll(items);
        items.forEach(it -> scope.addSelectItem(fromNode(it)));

      } else {
        modifiedSelectItems.add(itemNode);
        scope.addSelectItem(fromNode(itemNode));
      }
    }

    querySpec.put(QUERY_SPEC_SELECT_ITEMS, modifiedSelectItems);
    querySpec.relinkAll();
    modifiedSelectItems.forEach(scope::setScope);
  }

  private SelectItem fromNode(SQLNode node) {
    assert node.type() == Type.SELECT_ITEM;

    final String alias = node.get(SELECT_ITEM_ALIAS);
    final SQLNode expr = node.get(SELECT_ITEM_EXPR);

    final SelectItem item = new SelectItem();
    item.setNode(node);
    item.setExpr(expr);
    item.setAlias(alias);
    if (exprKind(expr) == Kind.COLUMN_REF)
      item.setSimpleName(expr.get(COLUMN_REF_COLUMN).get(COLUMN_NAME_COLUMN));

    return item;
  }

  private List<SQLNode> fromWildcard(SQLNode node) {
    assert exprKind(node) == Kind.WILDCARD;
    final QueryScope scope = node.get(RESOLVED_QUERY_SCOPE);
    final SQLNode tableId = node.get(WILDCARD_TABLE);

    if (tableId != null) {
      // qualified
      final String tableName = tableId.get(TABLE_NAME_TABLE);
      final TableSource tableSource = scope.resolveTable(tableName);
      if (tableSource != null) return allFromTableSource(tableSource);

      log(LOG, WARNING, "failed to resolve selection {3}", stmt, node);
      return Collections.emptyList();

    } else
      // unqualified
      return scope.tableSources().values().stream()
          .map(SelectionResolver::allFromTableSource)
          .flatMap(Collection::stream)
          .collect(Collectors.toList());
  }

  private static List<SQLNode> allFromTableSource(TableSource tableSource) {
    final List<String> selections = tableSource.namedSelections();
    final List<SQLNode> items = new ArrayList<>(selections.size());
    for (String namedSelection : selections)
      items.add(selectItem(columnRef(tableSource.name(), namedSelection), null));
    items.forEach(IdResolver::resolve);
    return items;
  }

  @Override
  public boolean resolve(Statement stmt) {
    LOG.log(
        System.Logger.Level.TRACE,
        "resolving selection for <{0}, {1}>",
        stmt.appName(),
        stmt.stmtId());
    this.stmt = stmt;
    stmt.parsed().accept(this);
    return true;
  }

  private static final Set<Class<? extends Resolver>> DEPENDENCIES = Set.of(TableResolver.class);

  @Override
  public Set<Class<? extends Resolver>> dependsOn() {
    return DEPENDENCIES;
  }
}
