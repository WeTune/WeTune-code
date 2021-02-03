package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.logic.LogicCtx;
import sjtu.ipads.wtune.symsolver.logic.Proposition;
import sjtu.ipads.wtune.symsolver.logic.Value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class BaseQueryBuilder implements QueryBuilder {
  private LogicCtx ctx;
  private SymMaker<TableSym> tables;
  private SymMaker<PickSym> picks;
  private SymMaker<PredicateSym> preds;

  private char nextTupleName = 'a';

  @Override
  public synchronized Query build(LogicCtx ctx, int tblBase, int pickBase, int predBase) {
    tables = new SymMaker<>(ctx, TableSym::of, tblBase);
    picks = new SymMaker<>(ctx, PickSym::of, pickBase);
    preds = new SymMaker<>(ctx, PredicateSym::of, predBase);

    this.ctx = ctx;
    this.nextTupleName = 'a';

    final Function<Value, Proposition> semantic = semantic(); // don't inline this variable

    return new QueryImpl(
        tables.syms().toArray(TableSym[]::new),
        picks.syms().toArray(PickSym[]::new),
        preds.syms().toArray(PredicateSym[]::new),
        semantic);
  }

  protected LogicCtx ctx() {
    return ctx;
  }

  protected TableSym makeTable() {
    return tables.make();
  }

  protected PickSym makePick() {
    return picks.make();
  }

  protected PredicateSym makePredicate() {
    return preds.make();
  }

  protected Value makeTuple() {
    return ctx.makeTuple(String.valueOf(nextTupleName++));
  }

  protected abstract Function<Value, Proposition> semantic();

  private static class SymMaker<T extends Sym> {
    private final Collection<T> syms;
    private final Supplier<T> maker;
    private final LogicCtx ctx;
    private int nextId;

    private SymMaker(LogicCtx ctx, Supplier<T> maker, int startId) {
      this.ctx = ctx;
      this.maker = maker;
      this.syms = new ArrayList<>();
      this.nextId = startId;
    }

    private T bindFunc(T sym) {
      sym.setFunc(ctx.makeFunc(sym));
      return sym;
    }

    private T setIndex(T sym) {
      sym.setIndex(nextId++);
      return sym;
    }

    private T make() {
      final T t = bindFunc(setIndex(maker.get()));
      syms.add(t);
      return t;
    }

    private Collection<T> syms() {
      return syms;
    }
  }

  private static class QueryImpl implements Query {
    private final TableSym[] tables;
    private final PickSym[] picks;
    private final PredicateSym[] preds;
    private final Function<Value, Proposition> semantic;

    private QueryImpl(
        TableSym[] tables,
        PickSym[] picks,
        PredicateSym[] preds,
        Function<Value, Proposition> semantic) {
      this.tables = tables;
      this.picks = picks;
      this.preds = preds;
      this.semantic = semantic;

      for (TableSym table : tables) table.setScope(this);
      for (PickSym pick : picks) pick.setScope(this);
      for (PredicateSym pred : preds) pred.setScope(this);
    }

    @Override
    public TableSym[] tables() {
      return tables;
    }

    @Override
    public PickSym[] picks() {
      return picks;
    }

    @Override
    public PredicateSym[] preds() {
      return preds;
    }

    @Override
    public Proposition contains(Value v) {
      return semantic.apply(v);
    }
  }
}
