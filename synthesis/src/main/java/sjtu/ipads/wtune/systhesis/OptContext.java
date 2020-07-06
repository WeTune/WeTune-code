package sjtu.ipads.wtune.systhesis;

import sjtu.ipads.wtune.common.attrs.Attrs;
import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.*;

public class OptContext implements Attrs<OptContext> {
  private final Statement originalStmt;
  private Statement referenceStmt;

  private List<Statement> output = new ArrayList<>();

  public static final Attrs.Key<Set<SQLNode>> REF_PRIMITIVE_PREDICATE_CACHE_KEY =
      Attrs.key2("synthesis.predicate.cache.ref_primitive", Set.class);

  public OptContext(Statement originalStmt) {
    this.originalStmt = originalStmt;
  }

  public Statement originalStmt() {
    return originalStmt;
  }

  public Statement referenceStmt() {
    return referenceStmt;
  }

  public void setReferenceStmt(Statement referenceStmt) {
    this.referenceStmt = referenceStmt;
    remove(REF_PRIMITIVE_PREDICATE_CACHE_KEY);
  }

  public List<Statement> output() {
    return output;
  }

  public Stage outputStage() {
    return Stage.listCollector(output);
  }

  private final Map<String, Object> directAttrs = new HashMap<>();

  @Override
  public Map<String, Object> directAttrs() {
    return directAttrs;
  }
}
