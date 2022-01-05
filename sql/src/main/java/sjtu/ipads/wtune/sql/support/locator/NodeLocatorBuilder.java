package sjtu.ipads.wtune.sql.support.locator;

import sjtu.ipads.wtune.sql.ast1.FieldDomain;
import sjtu.ipads.wtune.sql.ast1.SqlNode;
import sjtu.ipads.wtune.sql.ast1.SqlNodes;

import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import static java.util.Objects.checkFromToIndex;
import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.FuncUtils.pred;

public class NodeLocatorBuilder {
  private boolean scoped;
  private boolean bottomUp;
  private Predicate<SqlNode> criteria;
  private Predicate<SqlNode> brake;

  public NodeLocatorBuilder scoped() {
    this.scoped = true;
    return this;
  }

  public NodeLocatorBuilder bottomUp() {
    this.bottomUp = true;
    return this;
  }

  public NodeLocatorBuilder accept(FieldDomain domain) {
    requireNonNull(domain);
    final Predicate<SqlNode> criterion = domain::isInstance;
    if (this.criteria == null) criteria = criterion;
    else criteria = criteria.and(criterion);
    return this;
  }

  public NodeLocatorBuilder accept(Predicate<SqlNode> criterion) {
    requireNonNull(criterion);
    if (criteria == null) criteria = criterion;
    else criteria = criteria.and(criterion);
    return this;
  }

  public NodeLocatorBuilder stopIf(Predicate<SqlNode> stopAt) {
    requireNonNull(stopAt);
    if (brake == null) brake = stopAt;
    else brake = brake.and(stopAt);
    return this;
  }

  public NodeLocatorBuilder stopIfNot(FieldDomain domain) {
    requireNonNull(domain);
    if (brake == null) brake = pred(domain::isInstance).negate();
    else brake = brake.and(pred(domain::isInstance).negate());
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

  private NodeLocator mkLocator(int expectedNodes) {
    final Predicate<SqlNode> criteria = this.criteria;
    final Predicate<SqlNode> brake = this.brake;

    return new NodeLocator(scoped, bottomUp, expectedNodes) {
      @Override
      protected boolean shouldStop(SqlNode node) {
        return brake != null && brake.test(node);
      }

      @Override
      protected boolean shouldAccept(SqlNode node) {
        return criteria == null || criteria.test(node);
      }
    };
  }
}
