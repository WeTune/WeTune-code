package sjtu.ipads.wtune.systhesis.relation;

import sjtu.ipads.wtune.common.utils.Pair;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.analyzer.RelationGraphAnalyzer;
import sjtu.ipads.wtune.stmt.attrs.Relation;
import sjtu.ipads.wtune.stmt.attrs.RelationGraph;
import sjtu.ipads.wtune.stmt.statement.Statement;
import sjtu.ipads.wtune.systhesis.Stage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RelationMutation extends Stage {
  private final RelationGraph graph;
  private final List<RelationMutator> mutatorQueue;
  private final Set<Pair<Relation, Class<? extends RelationMutator>>> mutatorSet;
  private final Set<Relation> mutated;

  private RelationMutation(RelationGraph graph) {
    this.graph = graph;
    this.mutatorQueue = new ArrayList<>(graph.graph().nodes().size());
    this.mutatorSet = new HashSet<>(graph.graph().nodes().size());
    this.mutated = new HashSet<>(graph.graph().nodes().size());
  }

  public static RelationMutation build(Statement stmt) {
    return new RelationMutation(stmt.analyze(RelationGraphAnalyzer.class));
  }

  public boolean feed0(Statement stmt) {
    registerApplicableMutators(stmt.parsed(), graph);
    return recMutate(stmt, 0);
  }

  private boolean registerMutator(Relation target, Class<? extends RelationMutator> cls) {
    final Pair<Relation, Class<? extends RelationMutator>> pair = Pair.of(target, cls);
    if (!mutatorSet.add(pair)) return false;

    final RelationMutator mutator;
    if (cls == ExposeDerivedTableSource.class) {
      mutator = new ExposeDerivedTableSource(graph, target);
    } else if (cls == InlineSubquery.class) {
      mutator = new InlineSubquery(graph, target);
    } else if (cls == ReduceTableSource.class) {
      mutator = new ReduceTableSource(graph, target);
    } else return false;

    mutatorQueue.add(mutator);
    return true;
  }

  int registerApplicableMutators(SQLNode root, RelationGraph graph) {
    final Set<Relation> relations = graph.graph().nodes();
    final int size = mutatorQueue.size();
    for (Relation relation : relations) {
      if (ExposeDerivedTableSource.canExpose(root, relation)) {
        registerMutator(relation, ExposeDerivedTableSource.class);
      }
      if (InlineSubquery.canInline(root, graph, relation)) {
        registerMutator(relation, InlineSubquery.class);
      }
      if (ReduceTableSource.canReduce(root, graph, relation)) {
        registerMutator(relation, ReduceTableSource.class);
      }
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

  private boolean recMutate(Statement stmt, int i) {
    if (i >= mutatorQueue.size()) return offer(stmt);

    if (!recMutate(stmt, i + 1)) return false;

    final RelationMutator mutator = mutatorQueue.get(i);
    if (!mutator.isValid(stmt.parsed())) return true;

    final Statement copy = stmt.copy();
    mutator.modifyGraph();
    mutator.modifyAST(copy, copy.parsed());
    mutated.add(mutator.target());

    final int newMutatorCount = registerApplicableMutators(copy.parsed(), graph);

    if (!recMutate(copy, i + 1)) return false;

    mutator.undoModifyGraph();
    mutated.remove(mutator.target());

    unregisterMutators(newMutatorCount);

    return true;
  }

  @Override
  public boolean feed(Object o) {
    return feed0((Statement) o);
  }
}
