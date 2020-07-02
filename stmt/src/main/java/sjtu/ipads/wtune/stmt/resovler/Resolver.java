package sjtu.ipads.wtune.stmt.resovler;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.statement.Statement;
import sjtu.ipads.wtune.stmt.utils.StmtHelper;

import java.util.Collections;
import java.util.Set;

public interface Resolver {
  /** @return if succeed */
  default boolean resolve(Statement stmt) {
    return resolve(stmt, stmt.parsed());
  }

  /** @return if succeed */
  default boolean resolve(Statement stmt, SQLNode node) {
    return resolve(stmt);
  }

  default Set<Class<? extends Resolver>> dependsOn() {
    return Collections.emptySet();
  }

  Set<Class<? extends Resolver>> STANDARD_RESOLVERS =
      Set.of(
          IdResolver.class,
          BoolPrimitiveResolver.class,
          QueryScopeResolver.class,
          TableResolver.class,
          SelectionResolver.class,
          ColumnResolver.class,
          JoinConditionResolver.class);

  static Resolver getResolver(Class<? extends Resolver> cls) {
    final Resolver singleton = StmtHelper.getSingleton(cls);
    return singleton != null ? singleton : StmtHelper.newInstance(cls);
  }
}
