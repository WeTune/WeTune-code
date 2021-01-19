package sjtu.ipads.wtune.superopt.solving;

import sjtu.ipads.wtune.superopt.core.Graph;
import sjtu.ipads.wtune.superopt.internal.GraphVisitor;
import sjtu.ipads.wtune.superopt.operator.*;
import sjtu.ipads.wtune.symsolver.core.BaseQueryBuilder;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.PredicateSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;
import sjtu.ipads.wtune.symsolver.logic.Proposition;
import sjtu.ipads.wtune.symsolver.logic.Value;

import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Function;

import static sjtu.ipads.wtune.common.utils.Commons.*;

public class Semantic extends BaseQueryBuilder implements GraphVisitor {
  private final Graph q;
  private final Deque<Relation> stack;

  private Semantic(Graph q) {
    this.q = q;
    this.stack = new LinkedList<>();
  }

  public static Semantic build(Graph q) {
    return new Semantic(q);
  }

  private TableSym[][] makeViableSources(TableSym[] visible, boolean isJoinKey) {
    if (isJoinKey) {
      final TableSym[][] viable = new TableSym[visible.length][1];
      for (int i = 0; i < visible.length; i++) viable[i][0] = visible[i];
      return viable;
    } else {
      final int count = 1 << visible.length;
      final TableSym[][] viable = new TableSym[count][];
      for (int i = count - 1; i >= 0; --i) viable[i] = maskArray(visible, i);
      return viable;
    }
  }

  @Override
  public void leaveInput(Input input) {
    stack.push(new Relation(tableSym(input.table())));
  }

  @Override
  public void leaveInnerJoin(InnerJoin op) {
    final Relation inR = stack.pop(), inL = stack.pop();
    final PickSym pickL = pickSym(op.leftFields()), pickR = pickSym(op.rightFields());
    final TableSym[] visibleL = inL.visibleSources(), visibleR = inR.visibleSources();

    pickL.setVisibleSources(visibleL);
    pickR.setVisibleSources(visibleR);
    pickL.setViableSources(makeViableSources(visibleL, true));
    pickR.setViableSources(makeViableSources(visibleR, true));
    pickL.setJoined(pickR);

    final TableSym[] visible = arrayConcat(visibleL, visibleR);
    final Value l = newTuple(), r = newTuple();
    final Proposition inCond = inL.contains(l).and(inR.contains(r));
    final Proposition joinCond = pickL.apply(l).equalsTo(pickR.apply(r));
    final Value[] bound = asArray(l, r);
    final Function<Value, Proposition> cond =
        x -> ctx().makeExists(bound, inCond.and(joinCond).and(x.equalsTo(ctx().makeCombine(l, r))));

    stack.push(new Relation(visible, cond));
  }

  @Override
  public void leavePlainFilter(PlainFilter op) {
    final Relation in = stack.pop();
    final PickSym pick = pickSym(op.fields());
    final PredicateSym pred = predSym(op.predicate());
    final TableSym[] visible = in.visibleSources();

    pick.setVisibleSources(visible);
    pick.setViableSources(makeViableSources(visible, false));

    final Function<Value, Proposition> predicate =
        x -> in.contains(x).and((Proposition) pred.apply(x));

    stack.push(new Relation(visible, predicate));
  }

  @Override
  public void leaveProj(Proj op) {
    final Relation in = stack.pop();
    final PickSym pick = pickSym(op.fields());
    final TableSym[] visible = in.visibleSources();

    pick.setVisibleSources(visible);
    pick.setViableSources(makeViableSources(visible, false));

    final Value tuple = newTuple();
    final Function<Value, Proposition> predicate =
        x -> ctx().makeExists(tuple, in.contains(tuple).and(x.equalsTo(pick.apply(tuple))));

    stack.push(new Relation(visible, predicate));
  }

  @Override
  public void leaveSubqueryFilter(SubqueryFilter op) {
    final Relation sub = stack.pop(), in = stack.pop();
    final PickSym pick = pickSym(op.fields());
    final TableSym[] visible = in.visibleSources();

    pick.setVisibleSources(visible);
    pick.setViableSources(makeViableSources(visible, false));

    final Function<Value, Proposition> predicate = x -> in.contains(x).and(sub.contains(x));
    // actually multiple-output subquery should be rule out earlier. See Heuristic::prune
    stack.push(new Relation(visible, predicate));
  }

  @Override
  public void leaveUnion(Union op) {
    final Relation right = stack.pop(), left = stack.pop();
    final TableSym[] visible = left.visibleSources;

    final Function<Value, Proposition> predicate = x -> left.contains(x).or(right.contains(x));

    stack.push(new Relation(visible, predicate));
  }

  @Override
  protected Function<Value, Proposition> semantic() {
    stack.clear();
    q.acceptVisitor(this);
    return stack.peek()::contains;
  }

  private static final class Relation {
    private final TableSym[] visibleSources;
    private final Function<Value, Proposition> cond;

    private Relation(TableSym table) {
      visibleSources = asArray(table);
      cond = x -> (Proposition) table.apply(x);
    }

    private Relation(TableSym[] visibleSources, Function<Value, Proposition> cond) {
      this.visibleSources = visibleSources;
      this.cond = cond;
    }

    public TableSym[] visibleSources() {
      return visibleSources;
    }

    public Proposition contains(Value x) {
      return cond.apply(x);
    }
  }
}
