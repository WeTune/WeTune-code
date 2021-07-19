package sjtu.ipads.wtune.sqlparser.plan1;

import java.util.List;

public interface RefBag extends List<Ref> {
  static RefBag empty() {
    return RefBagImpl.EMPTY;
  }

  static RefBag mk(List<Ref> refs) {
    if (refs instanceof RefBag) return (RefBag) refs;
    else return new RefBagImpl(refs);
  }
}
