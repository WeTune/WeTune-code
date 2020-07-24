package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.attrs.QueryScope;
import sjtu.ipads.wtune.stmt.attrs.TableSource;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.System.Logger.Level.WARNING;
import static sjtu.ipads.wtune.sqlparser.SQLExpr.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.stmt.attrs.SelectItem.fromNode;
import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.RESOLVED_QUERY_SCOPE;

/**
 * Resolve output column (i.e. select item) of query scope, meanwhile expand wildcard expression.
 */
public class SelectionResolver implements Resolver, SQLVisitor {
  private static final System.Logger LOG = System.getLogger("Stmt.Resolver.Selection");
  private static final Set<Class<? extends Resolver>> DEPENDENCIES = Set.of(TableResolver.class);

  private Statement stmt;

  @Override
  public Set<Class<? extends Resolver>> dependsOn() {
    return DEPENDENCIES;
  }

  @Override
  public boolean resolve(Statement stmt) {
    this.stmt = stmt;
    stmt.parsed().accept(this);
    stmt.parsed().setDbTypeRec(stmt.parsed().dbType());
    return true;
  }

  @Override
  public void leaveQuerySpec(SQLNode querySpec) {
    final QueryScope scope = querySpec.get(RESOLVED_QUERY_SCOPE);

    final List<SQLNode> selectItems = querySpec.get(QUERY_SPEC_SELECT_ITEMS);
    final List<SQLNode> modifiedSelectItems = new ArrayList<>(selectItems.size());

    for (SQLNode itemNode : selectItems) {
      final SQLNode expr = itemNode.get(SELECT_ITEM_EXPR);
      if (exprKind(expr) == Kind.WILDCARD) modifiedSelectItems.addAll(expandWildcard(expr));
      else modifiedSelectItems.add(itemNode);
    }

    querySpec.put(QUERY_SPEC_SELECT_ITEMS, modifiedSelectItems);
    querySpec.relinkAll();

    for (SQLNode item : modifiedSelectItems) {
      scope.setScope(item);
      scope.addSelectItem(fromNode(item));
    }
  }

  private List<SQLNode> expandWildcard(SQLNode node) {
    assert exprKind(node) == Kind.WILDCARD;
    final QueryScope scope = node.get(RESOLVED_QUERY_SCOPE);
    final SQLNode tableId = node.get(WILDCARD_TABLE);

    if (tableId != null) {
      // qualified
      final String tableName = tableId.get(TABLE_NAME_TABLE);
      final TableSource tableSource = scope.resolveTable(tableName);
      if (tableSource != null) return selectItemsFrom(tableSource);

      LOG.log(
          WARNING,
          "unresolved wildcard selection {3}\n{0}\n{1}",
          stmt,
          stmt.parsed().toString(false),
          node);
      return Collections.emptyList();

    } else
      // unqualified
      return scope.tableSources().values().stream()
          .map(SelectionResolver::selectItemsFrom)
          .flatMap(Collection::stream)
          .collect(Collectors.toList());
  }

  private static List<SQLNode> selectItemsFrom(TableSource tableSource) {
    final List<String> selections = tableSource.namedSelections();
    final List<SQLNode> items = new ArrayList<>(selections.size());
    for (String selection : selections)
      items.add(selectItem(columnRef(tableSource.name(), selection), null));
    items.forEach(IdResolver::resolve);
    return items;
  }
}
