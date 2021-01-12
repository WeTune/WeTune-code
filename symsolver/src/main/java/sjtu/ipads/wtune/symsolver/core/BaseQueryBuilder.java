package sjtu.ipads.wtune.symsolver.core;

import sjtu.ipads.wtune.symsolver.logic.LogicCtx;
import sjtu.ipads.wtune.symsolver.logic.Proposition;
import sjtu.ipads.wtune.symsolver.logic.Value;
import sjtu.ipads.wtune.symsolver.utils.SimpleScoped;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static sjtu.ipads.wtune.common.utils.FuncUtils.stream;
import static sjtu.ipads.wtune.symsolver.core.Indexed.number;

public abstract class BaseQueryBuilder implements QueryBuilder {
  private LogicCtx ctx;
  private final Map<Scoped, TableSym> tableSyms;
  private final Map<Scoped, PickSym> pickSyms;
  private final Map<Scoped, PredicateSym> predSyms;

  protected TableSym[] tables;
  protected PickSym[] picks;
  protected PredicateSym[] preds;
  protected Value[] tuples;

  protected BaseQueryBuilder() {
    this.tableSyms = new LinkedHashMap<>();
    this.pickSyms = new LinkedHashMap<>();
    this.predSyms = new LinkedHashMap<>();
  }

  @Override
  public LogicCtx ctx() {
    return ctx;
  }

  @Override
  public TableSym tableSym(Scoped owner) {
    return tableSyms.computeIfAbsent(owner, TableSym::of);
  }

  @Override
  public PickSym pickSym(Scoped owner) {
    return pickSyms.computeIfAbsent(owner, PickSym::of);
  }

  @Override
  public PredicateSym predSym(Scoped owner) {
    return predSyms.computeIfAbsent(owner, PredicateSym::of);
  }

  @Override
  public Query build(
      LogicCtx ctx, String name, int tblIdxStart, int pickIdxStart, int predIdxStart) {
    this.tableSyms.clear();
    this.pickSyms.clear();
    this.predSyms.clear();

    this.ctx = ctx;
    this.tables = number(makeTables(), tblIdxStart);
    this.picks = number(makePicks(), pickIdxStart);
    this.preds = number(makePredicates(), predIdxStart);
    this.tuples = ctx.makeTuples(numTables(), name);

    prepare();

    bindFuncs(tables);
    bindFuncs(picks);
    bindFuncs(preds);

    return new BaseQuery(tables, picks, preds, tuples, output(), condition());
  }

  private TableSym[] makeTables() {
    final List<? extends Scoped> ts = tablePlaceholders();
    if (ts != null) return stream(ts).map(this::tableSym).toArray(TableSym[]::new);
    else
      return IntStream.range(0, numTables())
          .mapToObj(it -> new SimpleScoped(this))
          .map(this::tableSym)
          .toArray(TableSym[]::new);
  }

  private PickSym[] makePicks() {
    final List<? extends Scoped> ps = pickPlaceholders();
    if (ps != null) return stream(ps).map(this::pickSym).toArray(PickSym[]::new);
    return IntStream.range(0, numPicks())
        .mapToObj(it -> new SimpleScoped(this))
        .map(this::pickSym)
        .toArray(PickSym[]::new);
  }

  private PredicateSym[] makePredicates() {
    final List<? extends Scoped> ps = predicatePlaceholders();
    if (ps != null) return stream(ps).map(this::predSym).toArray(PredicateSym[]::new);
    return IntStream.range(0, numPreds())
        .mapToObj(it -> new SimpleScoped(this))
        .map(this::predSym)
        .toArray(PredicateSym[]::new);
  }

  private void bindFuncs(Sym[] syms) {
    for (Sym sym : syms) sym.setFunc(ctx.makeFunc(sym));
  }

  @Override
  public int numTables() {
    return tablePlaceholders().size();
  }

  @Override
  public int numPicks() {
    return pickPlaceholders().size();
  }

  @Override
  public int numPreds() {
    return predicatePlaceholders().size();
  }

  protected List<? extends Scoped> tablePlaceholders() {
    return null;
  }

  protected List<? extends Scoped> pickPlaceholders() {
    return null;
  }

  protected List<? extends Scoped> predicatePlaceholders() {
    return null;
  }

  protected abstract void prepare();

  private static class BaseQuery implements Query {
    private final TableSym[] tables;
    private final PickSym[] picks;
    private final PredicateSym[] preds;
    private final Value[] tuples;
    private final Value[] output;
    private final Proposition condition;

    private BaseQuery(
        TableSym[] tables,
        PickSym[] picks,
        PredicateSym[] preds,
        Value[] tuples,
        Value[] output,
        Proposition condition) {
      this.tables = tables;
      this.picks = picks;
      this.preds = preds;
      this.tuples = tuples;
      this.output = output;
      this.condition = condition;
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
    public Value[] tuples() {
      return tuples;
    }

    @Override
    public Value[] output() {
      return output;
    }

    @Override
    public Proposition condition() {
      return condition;
    }
  }
}
