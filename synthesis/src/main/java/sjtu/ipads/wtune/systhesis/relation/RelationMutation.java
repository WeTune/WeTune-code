package sjtu.ipads.wtune.systhesis.relation;

import sjtu.ipads.wtune.common.utils.Pair;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.attrs.Relation;
import sjtu.ipads.wtune.stmt.attrs.RelationGraph;
import sjtu.ipads.wtune.stmt.statement.Statement;
import sjtu.ipads.wtune.systhesis.Stage;
import sjtu.ipads.wtune.systhesis.SynthesisContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RelationMutation extends Stage {
  private final SynthesisContext ctx;

  private final RelationGraph graph;
  private final List<RelationMutator> mutatorQueue;
  private final Set<Pair<Relation, Class<? extends RelationMutator>>> mutatorSet;
  private final Set<Relation> mutated;

  private final List<Statement> produced = new ArrayList<>();
  private final Set<String> known = new HashSet<>();

  private RelationMutation(SynthesisContext ctx, RelationGraph graph) {
    this.ctx = ctx;
    this.graph = graph;
    this.mutatorQueue = new ArrayList<>(graph.graph().nodes().size());
    this.mutatorSet = new HashSet<>(graph.graph().nodes().size());
    this.mutated = new HashSet<>(graph.graph().nodes().size());
  }

  public static RelationMutation build(SynthesisContext ctx, Statement stmt) {
    return new RelationMutation(ctx, stmt.relationGraph());
  }

  @Override
  public boolean feed(Object o) {
    // relation mutation is independent from the reference.
    // So we can do this for only one time, cache the results
    // and then reuse them when reference changes
    final long start = System.currentTimeMillis();
    boolean ret = true;
    if (produced.isEmpty()) ret = feed0((Statement) o);
    else
      for (Statement statement : produced)
        if (!offer(statement)) {
          ret = false;
          break;
        }

    ctx.output().relationElapsed += System.currentTimeMillis() - start;
    mutatorSet.clear();

    return ret;
  }

  public boolean offer0(Object obj) {
    final Statement output = (Statement) obj;
    if (known.add(output.parsed().toString())) {
      produced.add(output);
      return super.offer(output);
    }
    return true;
  }

  private boolean feed0(Statement stmt) {
    registerApplicableMutators(stmt.parsed(), graph);
    return recMutate(stmt, 0);
  }

  private boolean registerMutator(
      SQLNode root, RelationGraph graph, Relation target, Class<? extends RelationMutator> cls) {
    final Pair<Relation, Class<? extends RelationMutator>> pair = Pair.of(target, cls);
    if (mutatorSet.contains(pair)) return false;

    final RelationMutator mutator;
    if (cls == ExposeDerivedTableSource.class && ExposeDerivedTableSource.canExpose(root, target)) {
      mutator = new ExposeDerivedTableSource(graph, target);
    } else if (cls == InlineSubquery.class && InlineSubquery.canInline(root, graph, target)) {
      mutator = new InlineSubquery(graph, target);
    } else if (cls == ReduceTableSource.class && ReduceTableSource.canReduce(root, graph, target)) {
      mutator = new ReduceTableSource(graph, target);
    } else return false;

    mutatorSet.add(pair);
    mutatorQueue.add(mutator);
    return true;
  }

  int registerApplicableMutators(SQLNode root, RelationGraph graph) {
    final Set<Relation> relations = graph.graph().nodes();
    final int size = mutatorQueue.size();
    for (Relation relation : relations) {
      registerMutator(root, graph, relation, ExposeDerivedTableSource.class);
      registerMutator(root, graph, relation, InlineSubquery.class);
      registerMutator(root, graph, relation, ReduceTableSource.class);
    }
    return mutatorQueue.size() - size;
  }

  void unregisterMutators(int i) {
    final List<RelationMutator> subList =
        mutatorQueue.subList(mutatorQueue.size() - i, mutatorQueue.size());
    for (RelationMutator mutator : subList)
      mutatorSet.remove(Pair.of(mutator.target(), mutator.getClass()));
    subList.clear();
  }

  private boolean recMutate(Statement stmt, int nextMutator) {
    if (nextMutator >= mutatorQueue.size()) return offer0(stmt);

    if (!recMutate(stmt, nextMutator + 1)) return false;

    final RelationMutator mutator = mutatorQueue.get(nextMutator);
    if (!mutator.isValid(stmt.parsed())) return true;

    final Statement copy = stmt.copy();
    mutator.modifyGraph(copy.parsed());
    mutator.modifyAST(copy, copy.parsed());
    mutated.add(mutator.target());

    final int newMutatorCount = registerApplicableMutators(copy.parsed(), graph);

    if (!recMutate(copy, nextMutator + 1)) return false;

    mutator.undoModifyGraph();
    mutated.remove(mutator.target());

    unregisterMutators(newMutatorCount);

    return true;
  }
}
