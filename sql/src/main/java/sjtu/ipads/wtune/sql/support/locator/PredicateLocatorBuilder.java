package sjtu.ipads.wtune.sql.support.locator;

import sjtu.ipads.wtune.sql.ast1.SqlNode;
import sjtu.ipads.wtune.sql.ast1.SqlNodes;

public class PredicateLocatorBuilder {
  private boolean scoped;
  private boolean bottomUp;
  private boolean primitive;

  public PredicateLocatorBuilder scoped() {
    this.scoped = true;
    return this;
  }

  public PredicateLocatorBuilder bottomUp() {
    this.bottomUp = true;
    return this;
  }

  public PredicateLocatorBuilder primitive() {
    this.primitive = true;
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
    return new PredicateLocator(scoped, bottomUp, primitive, expectedNodes);
  }
}
