package sjtu.ipads.wtune.systhesis;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.mutator.TupleElementsNormalizer;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.*;

import static sjtu.ipads.wtune.systhesis.Synthesis.*;
import static sjtu.ipads.wtune.systhesis.TemplatizeSQLFormatter.templatize;

public class SynthesisContext implements Attrs<SynthesisContext> {
  private final Statement originalStmt;
  private Statement referenceStmt;

  private Stage stageHead;
  private Stage stageTail;

  private final Set<String> known = new HashSet<>();
  private final List<Statement> candidates = new ArrayList<>();
  private final List<Statement> produced = new ArrayList<>();
  private final List<Statement> optimized = new ArrayList<>();

  public static final Attrs.Key<Set<SQLNode>> REF_PRIMITIVE_PREDICATE_CACHE_KEY =
      Attrs.key2("synthesis.predicate.cache.refPrimitive", Set.class);

  public SynthesisContext(Statement originalStmt) {
    this.originalStmt = originalStmt;
  }

  public boolean start() {
    stageTail.setNext(collector());
    return stageHead.feed(originalStmt);
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

  public void verifyCandidates() {
    if (candidates.size() > 1) optimized.addAll(Synthesis.verify(originalStmt, candidates));
  }

  private boolean collect(Statement stmt) {
    // make sure the unmodified one always be the head
    if (stmt == originalStmt && !candidates.isEmpty()) return true;
    if (!known.add(templatize(stmt.parsed()))) return true;

    candidates.add(stmt);
    if (stmt != originalStmt) produced.add(stmt);

    if (candidates.size() < CANDIDATES_BATCH_SIZE) return true;

    verifyCandidates();
    if (optimized.size() >= EXPECTED_OPTIMIZED) return false;

    candidates.clear();
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
