package sjtu.ipads.wtune.systhesis.predicate;

import com.google.common.collect.Sets;
import sjtu.ipads.wtune.common.utils.Pair;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.analyzer.BoolExprCollector;
import sjtu.ipads.wtune.stmt.statement.Statement;
import sjtu.ipads.wtune.systhesis.Stage;
import sjtu.ipads.wtune.systhesis.SynthesisContext;
import sjtu.ipads.wtune.systhesis.TemplatizeSQLFormatter;

import java.util.*;

import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.NODE_ID;

public class PredicateMutation extends Stage {
  private final SynthesisContext ctx;
  private final List<PredicateMutator> mutatorQueue;
  private final Set<Pair<Long, Long>> replacedPair;
  private final int maxModCount;
  private final int maxDepth;

  private final Set<String> seenPredicates = new HashSet<>();

  public PredicateMutation(SynthesisContext ctx, int maxModCount, int maxDepth) {
    this.ctx = ctx;
    this.maxModCount = maxModCount;
    this.maxDepth = maxDepth;
    this.replacedPair = new HashSet<>();
    this.mutatorQueue = new ArrayList<>();
  }

  public static PredicateMutation build(SynthesisContext ctx, int maxModCount, int maxDepth) {
    return new PredicateMutation(ctx, maxModCount, maxDepth);
  }

  private static Set<SQLNode> collectPrimitivePredicates(SQLNode root) {
    return new HashSet<>(BoolExprCollector.collectPrimitive(root));
  }

  private Set<SQLNode> collectRefPrimitivePredicates(SQLNode thisRoot, SQLNode refRoot) {
    final Set<SQLNode> refPreds = collectPrimitivePredicates(refRoot);
    final Iterator<SQLNode> iter = refPreds.iterator();
    while (iter.hasNext()) {
      final SQLNode node = iter.next();
      final String template = TemplatizeSQLFormatter.templatize(node, true);
      if (!seenPredicates.add(template)) iter.remove();
    }
    return refPreds;
  }

  private boolean registerMutator(
      SQLNode target, SQLNode reference, Class<? extends PredicateMutator> cls) {

    final PredicateMutator mutator;
    if (cls == DisplacePredicate.class) {
      final Long targetId = target.get(NODE_ID);
      final Long referenceId = reference.get(NODE_ID);
      if (Objects.equals(targetId, referenceId)) return false;

      final Pair<Long, Long> pair = Pair.of(targetId, referenceId);
      if (replacedPair.contains(pair) || !DisplacePredicate.canDisplace(target, reference))
        return false;

      replacedPair.add(pair);
      mutator = new DisplacePredicate(target, reference);
    } else return false;

    mutatorQueue.add(mutator);
    return true;
  }

  private int registerApplicableMutators(SQLNode root) {
    if (ctx.referenceStmt() == null) return 0;
    final Set<SQLNode> targets = collectPrimitivePredicates(root);
    final Set<SQLNode> refs = collectRefPrimitivePredicates(root, ctx.referenceStmt().parsed());
    final int size = mutatorQueue.size();
    for (List<SQLNode> pair : Sets.cartesianProduct(targets, refs)) {
      final SQLNode target = pair.get(0);
      final SQLNode ref = pair.get(1);
      registerMutator(target, ref, DisplacePredicate.class);
    }
    return mutatorQueue.size() - size;
  }

  private void unregisterMutators(int count) {
    final List<PredicateMutator> mutators =
        mutatorQueue.subList(mutatorQueue.size() - count, mutatorQueue.size());
    for (PredicateMutator mutator : mutators)
      replacedPair.remove(Pair.of(mutator.target().get(NODE_ID), mutator.reference().get(NODE_ID)));
    mutators.clear();
  }

  /*  use mutual recursion to simulate recursive enumeration */

  private boolean recMutate0(Statement stmt, int nextMutatorIdx, int depth, int modCount) {
    // after a pass, re-resolve the stmt
    if (depth > 0) stmt.reResolve();
    // if max depth exceeded, then shift to next stage
    if (depth >= maxDepth) return offer(stmt);
    // register new applicable mutators
    final int newMutators = registerApplicableMutators(stmt.parsed());
    // start next pass
    final boolean ret = recMutate1(stmt, nextMutatorIdx, depth + 1, modCount);
    // unregister mutators and backtrace
    unregisterMutators(newMutators);

    return ret;
  }

  private boolean recMutate1(Statement stmt, int nextMutatorIdx, int depth, int modCount) {
    // max mod count is exceeded, stop here
    if (modCount >= maxModCount) {
      stmt.reResolve();
      return offer(stmt);
    }
    // the queue being drained indicates that current pass is over
    if (nextMutatorIdx >= mutatorQueue.size())
      return recMutate0(stmt, nextMutatorIdx, depth, modCount);

    // recurse with unmodified stmt
    if (!recMutate1(stmt, nextMutatorIdx + 1, depth, modCount)) return false;

    // modify
    final PredicateMutator mutator = mutatorQueue.get(nextMutatorIdx);
    if (!mutator.isValid(stmt.parsed())) return true;

    final Statement copy = stmt.copy();
    mutator.modifyAST(copy.parsed());

    // recurse with modified stmt
    return recMutate1(copy, nextMutatorIdx + 1, depth, modCount + 1);
  }

  @Override
  public boolean feed(Object o) {
    final long start = System.currentTimeMillis();
    boolean ret = feed0((Statement) o);
    ctx.output().predicateElapsed += System.currentTimeMillis() - start;
    return ret;
  }

  public boolean feed0(Statement stmt) {
    final boolean ret = recMutate0(stmt, 0, 0, 0);
    assert mutatorQueue.isEmpty();
    replacedPair.clear();
    return ret;
  }
}
