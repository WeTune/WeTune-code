package sjtu.ipads.wtune.superopt.util;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.procedure.TObjectIntProcedure;
import sjtu.ipads.wtune.superopt.plan.*;

public class PlaceholderNumbering implements PlanVisitor {
  private int nextPredId;
  private int nextPickId;
  private int nextTblId;

  private final TObjectIntMap<Placeholder> numbering;

  private PlaceholderNumbering() {
    this.numbering = new TObjectIntHashMap<>(16, 1.0f, -1);
  }

  public static PlaceholderNumbering build() {
    return new PlaceholderNumbering();
  }

  public void number(Plan... plans) {
    for (Plan plan : plans) plan.acceptVisitor(this);
  }

  public String nameOf(Placeholder placeholder) {
    return placeholder.tag() + numbering.get(placeholder);
  }

  public int numberOf(Placeholder placeholder) {
    return numbering.get(placeholder);
  }

  public Placeholder find(String name) {
    final Finder finder = new Finder(name);
    numbering.forEachEntry(finder);
    return finder.found;
  }

  private void addPlaceholder(Placeholder placeholder, int index) {
    numbering.put(placeholder, index);
  }

  @Override
  public boolean enterInnerJoin(InnerJoin op) {
    addPlaceholder(op.leftFields(), nextPickId++);
    addPlaceholder(op.rightFields(), nextPickId++);
    return true;
  }

  @Override
  public boolean enterLeftJoin(LeftJoin op) {
    addPlaceholder(op.leftFields(), nextPickId++);
    addPlaceholder(op.rightFields(), nextPickId++);
    return true;
  }

  @Override
  public boolean enterPlainFilter(PlainFilter op) {
    addPlaceholder(op.predicate(), nextPredId++);
    addPlaceholder(op.fields(), nextPickId++);
    return true;
  }

  @Override
  public boolean enterSubqueryFilter(SubqueryFilter op) {
    addPlaceholder(op.fields(), nextPickId++);
    return true;
  }

  @Override
  public boolean enterProj(Proj op) {
    addPlaceholder(op.fields(), nextPickId++);
    return true;
  }

  @Override
  public boolean enterInput(Input input) {
    addPlaceholder(input.table(), nextTblId++);
    return true;
  }

  private static class Finder implements TObjectIntProcedure<Placeholder> {
    private final String target;
    private Placeholder found;

    private Finder(String target) {
      this.target = target;
    }

    @Override
    public boolean execute(Placeholder placeholder, int index) {
      if (target.equals(placeholder.tag() + index)) {
        found = placeholder;
        return false;
      }
      return true;
    }
  }
}
