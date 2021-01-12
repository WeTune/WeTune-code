package sjtu.ipads.wtune.superopt.util;

import sjtu.ipads.wtune.superopt.core.Graph;
import sjtu.ipads.wtune.superopt.internal.GraphVisitor;
import sjtu.ipads.wtune.superopt.internal.Placeholder;
import sjtu.ipads.wtune.superopt.operator.*;

import java.util.HashMap;
import java.util.Map;

public class NumberPlaceholders implements GraphVisitor {
  private int nextPredId;
  private int nextPickId;
  private int nextTblId;

  private final Map<String, Placeholder> placeholders = new HashMap<>();

  public static Map<String, Placeholder> number(Graph... graphs) {
    final NumberPlaceholders number = new NumberPlaceholders();
    for (Graph graph : graphs) graph.acceptVisitor(number);
    return number.placeholders;
  }

  private void addPlaceholder(Placeholder placeholder) {
    placeholders.put(placeholder.toString(), placeholder);
  }

  @Override
  public boolean enterInnerJoin(InnerJoin op) {
    op.leftFields().setIndex(nextPickId++);
    op.rightFields().setIndex(nextPickId++);
    addPlaceholder(op.leftFields());
    addPlaceholder(op.rightFields());
    return true;
  }

  @Override
  public boolean enterLeftJoin(LeftJoin op) {
    op.leftFields().setIndex(nextPickId++);
    op.rightFields().setIndex(nextPickId++);
    addPlaceholder(op.leftFields());
    addPlaceholder(op.rightFields());
    return true;
  }

  @Override
  public boolean enterPlainFilter(PlainFilter op) {
    op.predicate().setIndex(nextPredId++);
    op.fields().setIndex(nextPickId++);
    addPlaceholder(op.predicate());
    addPlaceholder(op.fields());
    return true;
  }

  @Override
  public boolean enterSubqueryFilter(SubqueryFilter op) {
    op.fields().setIndex(nextPickId++);
    addPlaceholder(op.fields());
    return true;
  }

  @Override
  public boolean enterProj(Proj op) {
    op.fields().setIndex(nextPickId++);
    addPlaceholder(op.fields());
    return true;
  }

  @Override
  public boolean enterInput(Input input) {
    input.table().setIndex(nextTblId++);
    addPlaceholder(input.table());
    return true;
  }
}
