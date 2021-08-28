package sjtu.ipads.wtune.sqlparser.plan;

import java.util.*;

import static java.util.Objects.requireNonNull;

class RefBagImpl extends AbstractList<Ref> implements RefBag {
  private final List<Ref> refs;

  static final RefBag EMPTY = new RefBagImpl(Collections.emptyList());

  RefBagImpl(List<Ref> refs) {
    this.refs = requireNonNull(refs);
  }

  @Override
  public Ref get(int index) {
    return refs.get(index);
  }

  @Override
  public int size() {
    return refs.size();
  }

  @Override
  public Iterator<Ref> iterator() {
    return refs.iterator();
  }

  @Override
  public Spliterator<Ref> spliterator() {
    return refs.spliterator();
  }
}
