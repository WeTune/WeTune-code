package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.logic.LogicCtx;
import sjtu.ipads.wtune.symsolver.logic.Proposition;
import sjtu.ipads.wtune.symsolver.logic.Value;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class BaseQueryBuilder implements QueryBuilder {
  private LogicCtx ctx;
  private SymMaker<TableSym> tables;
  private SymMaker<PickSym> picks;
  private SymMaker<PredicateSym> preds;

  private char nextTupleName = 'a';

  @Override
  public LogicCtx ctx() {
    return ctx;
  }

  @Override
  public TableSym tableSym(Scoped owner) {
    return tables.make(owner);
  }

  @Override
  public PickSym pickSym(Scoped owner) {
    return picks.make(owner);
  }

  @Override
  public PredicateSym predSym(Scoped owner) {
    return preds.make(owner);
  }

  @Override
  public synchronized Query build(LogicCtx ctx, int tblBase, int pickBase, int predBase) {
    tables = new SymMaker<>(ctx, TableSym::of, tblBase);
    picks = new SymMaker<>(ctx, PickSym::of, pickBase);
    preds = new SymMaker<>(ctx, PredicateSym::of, predBase);

    this.ctx = ctx;
    this.nextTupleName = 'a';

    final Function<Value, Proposition> semantic = semantic(); // don't inline this variable

    return new BaseQuery(
        tables.syms().toArray(TableSym[]::new),
        picks.syms().toArray(PickSym[]::new),
        preds.syms().toArray(PredicateSym[]::new),
        semantic);
  }

  protected Value newTuple() {
    return ctx.makeTuple(String.valueOf(nextTupleName++));
  }

  protected abstract Function<Value, Proposition> semantic();

  private static class SymMaker<T extends Sym> {
    private final Map<Scoped, T> syms;
    private final Function<Scoped, T> maker;
    private final LogicCtx ctx;
    private int nextId;

    private SymMaker(LogicCtx ctx, Function<Scoped, T> maker, int startId) {
      this.ctx = ctx;
      this.maker = maker;
      this.syms = new HashMap<>();
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

    private T make(Scoped scoped) {
      return syms.computeIfAbsent(scoped, maker.andThen(this::setIndex).andThen(this::bindFunc));
    }

    private Collection<T> syms() {
      return syms.values();
    }
  }

  private static class BaseQuery implements Query {
    private final TableSym[] tables;
    private final PickSym[] picks;
    private final PredicateSym[] preds;
    private final Function<Value, Proposition> semantic;

    private BaseQuery(
        TableSym[] tables,
        PickSym[] picks,
        PredicateSym[] preds,
        Function<Value, Proposition> semantic) {
      this.tables = tables;
      this.picks = picks;
      this.preds = preds;
      this.semantic = semantic;
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
