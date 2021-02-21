package sjtu.ipads.wtune.superopt.fragment.symbolic.internal;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.procedure.TObjectIntProcedure;
import sjtu.ipads.wtune.superopt.fragment.*;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Numbering;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;

public class NumberingImpl implements OperatorVisitor, Numbering {
  private int nextPredicateId;
  private int nextPickId;
  private int nextTableId;

  private final TObjectIntMap<Placeholder> numbering;

  private NumberingImpl() {
    this.numbering = new TObjectIntHashMap<>(16, 1.0f, -1);
  }

  public static Numbering build() {
    return new NumberingImpl();
  }

  @Override
  public Numbering number(Fragment... fragments) {
    for (Fragment fragment : fragments) fragment.acceptVisitor(this);
    return this;
  }

  @Override
  public int numberOf(Placeholder placeholder) {
    return numbering.get(placeholder);
  }

  @Override
  public Placeholder placeholderOf(String name) {
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
    addPlaceholder(op.predicate(), nextPredicateId++);
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
    addPlaceholder(input.table(), nextTableId++);
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