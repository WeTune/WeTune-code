package sjtu.ipads.wtune.systhesis;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.mutator.TupleElementsNormalizer;
import sjtu.ipads.wtune.stmt.resolver.SimpleParamResolver;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.*;

import static sjtu.ipads.wtune.systhesis.Synthesis.CANDIDATES_BATCH_SIZE;
import static sjtu.ipads.wtune.systhesis.Synthesis.EXPECTED_OPTIMIZED;
import static sjtu.ipads.wtune.systhesis.TemplatizeSQLFormatter.templatize;

public class SynthesisContext implements Attrs<SynthesisContext> {
  private final Statement originalStmt;
  private Statement referenceStmt;

  private Stage stageHead;
  private Stage stageTail;

  private final SynthesisOutput output = new SynthesisOutput();

  private final Set<String> known = new HashSet<>();
  private final List<Statement> candidates = new ArrayList<>();
  private final List<Statement> produced = new ArrayList<>();
  private final List<Statement> optimized = output.optimized;

  public static final Attrs.Key<Set<SQLNode>> REF_PRIMITIVE_PREDICATE_CACHE_KEY =
      Attrs.key2("synthesis.predicate.cache.refPrimitive", Set.class);

  public SynthesisContext(Statement originalStmt) {
    output.base = this.originalStmt = originalStmt;
  }

  public boolean start() {
    stageTail.setNext(collector());
    final boolean ret = stageHead.feed(originalStmt);

    output.producedCount = produced.size();
    output.usedRefCount += 1;
    return ret;
  }

  public Statement originalStmt() {
    return originalStmt;
  }

  public Statement referenceStmt() {
    return referenceStmt;
  }

  public void addStage(Stage stage) {
    if (stageHead == null) stageHead = stage;
    if (stageTail != null) stageTail.setNext(stage);
    stageTail = stage;
  }

  public void setReferenceStmt(Statement referenceStmt) {
    this.referenceStmt = referenceStmt;
    referenceStmt.retrofitStandard();
    referenceStmt.mutate(TupleElementsNormalizer.class);
    referenceStmt.resolve(SimpleParamResolver.class);
    remove(REF_PRIMITIVE_PREDICATE_CACHE_KEY);
  }

  public List<Statement> candidates() {
    return candidates;
  }

  public List<Statement> optimized() {
    return optimized;
  }

  public List<Statement> produced() {
    return produced;
  }

  public SynthesisOutput output() {
    return output;
  }

  public void verifyCandidates() {
    if (candidates.size() > 1) {
      final long start = System.currentTimeMillis();
      Synthesis.verify(originalStmt, candidates, output);
      output.verificationElapsed += System.currentTimeMillis() - start;
    }
  }

  private boolean collect(Statement stmt) {
    // make sure the unmodified one always be the head
    if (stmt == originalStmt && !candidates.isEmpty()) return true;
    if (!known.add(templatize(stmt.parsed()))) return true;

    candidates.add(stmt);
    if (stmt != originalStmt) produced.add(stmt);

    if (candidates.size() < CANDIDATES_BATCH_SIZE) return true;

    verifyCandidates();

    candidates.clear();

    if (optimized.size() >= EXPECTED_OPTIMIZED) return false;

    // make sure the unmodified one always be the head
    candidates.add(originalStmt);
    return true;
  }

  public Stage collector() {
    return collector;
  }

  private final Stage collector =
      new Stage() {
        @Override
        public Stage next() {
          return null;
        }

        @Override
        public boolean feed(Object o) {
          return collect((Statement) o);
        }
      };

  private final Map<String, Object> directAttrs = new HashMap<>();

  @Override
  public Map<String, Object> directAttrs() {
    return directAttrs;
  }
}
