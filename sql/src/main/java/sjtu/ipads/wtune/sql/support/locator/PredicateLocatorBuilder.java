package sjtu.ipads.wtune.sql.support.locator;

import sjtu.ipads.wtune.common.field.FieldKey;
import sjtu.ipads.wtune.sql.ast1.SqlNode;
import sjtu.ipads.wtune.sql.ast1.SqlNodes;

import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

public class PredicateLocatorBuilder {
  private boolean scoped;
  private boolean bottomUp;

  public PredicateLocatorBuilder scoped() {
    this.scoped = true;
    return this;
  }

  public PredicateLocatorBuilder bottomUp(boolean bottomUp) {
    this.bottomUp = bottomUp;
    return this;
  }

  public SqlFinder finder() {
    return mkLocator(1);
  }

  public SqlGatherer gatherer() {
    return mkLocator(-1);
  }

  public SqlNode find(SqlNode node) {
    return finder().findNode(node);
  }

  public SqlNodes gather(SqlNode node) {
    return gatherer().gatherNodes(node);
  }

  private PredicateLocator mkLocator(int expectedNodes) {
    return new PredicateLocator(scoped, bottomUp, expectedNodes);
  }
}
