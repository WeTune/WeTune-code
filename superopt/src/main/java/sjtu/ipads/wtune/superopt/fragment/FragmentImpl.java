package sjtu.ipads.wtune.superopt.fragment;

import sjtu.ipads.wtune.common.utils.Lazy;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;

import static sjtu.ipads.wtune.superopt.fragment.FragmentUtils.*;

class FragmentImpl implements Fragment {
  private int id;
  private Op root;

  private final Lazy<Symbols> symbols;

  FragmentImpl(Op root, Symbols symbols, boolean setupContext) {
    this.root = root;
    this.symbols = Lazy.mk(symbols);
    if (setupContext) root.acceptVisitor(OpVisitor.traverse(it -> it.setFragment(this)));
  }

  FragmentImpl(Op root) {
    this.root = root;
    this.symbols = Lazy.mk(this::initSymbols);
    // must be the `root()` accessor instead of `root`
  }

  static Fragment parse(String str, SymbolNaming naming) {
    return parse(str, naming, false);
  }

  static Fragment parse(String str, SymbolNaming naming, boolean backwardCompatible) {
    final String[] opStrs = str.split("[(),]+");
    final Deque<Op> operators = new ArrayDeque<>(opStrs.length);
    final Map<Op, String[]> names = naming == null ? null : new IdentityHashMap<>();

    for (int i = opStrs.length - 1; i >= 0; i--) {
      final String[] fields = opStrs[i].split("[<> ]+");
      final Op op = Op.parse(fields[0]);

      for (int j = 0; j < op.kind().numPredecessors(); j++) op.setPredecessor(j, operators.pop());
      operators.push(op);

      if (names != null) names.put(op, fields);
    }

    final Fragment fragment = setupFragment(new FragmentImpl(operators.pop()));
    if (names != null) names.forEach((op, ns) -> bindNames(op, ns, naming, backwardCompatible));

    return fragment;
  }

  void setRoot0(Op root) {
    this.root = root;
  }

  @Override
  public int id() {
    return id;
  }

  @Override
  public Op root() {
    return root;
  }

  @Override
  public void setId(int id) {
    this.id = id;
  }

  @Override
  public void setRoot(Op root) {
    if (this.root != null)
      throw new IllegalStateException("cannot change fragment's root once set");

    this.root = root;
  }

  @Override
  public Symbols symbols() {
    return symbols.get();
  }

  @Override
  public FragmentImpl copy() {
    return new FragmentImpl(root == null ? null : root.copyTree());
  }

  @Override
  public void acceptVisitor(OpVisitor visitor) {
    if (root != null) root.acceptVisitor(visitor);
  }

  @Override
  public StringBuilder stringify(SymbolNaming naming, StringBuilder builder) {
    return root == null ? builder : FragmentUtils.structuralToString(root, naming, builder);
  }

  @Override
  public String toString() {
    return root == null ? "" : stringify(null, new StringBuilder()).toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Fragment)) return false;
    return structuralEq(root(), ((Fragment) o).root());
  }

  @Override
  public int hashCode() {
    return root == null ? 0 : structuralHash(root);
  }

  private Symbols initSymbols() {
    final Symbols symbols = Symbols.mk();
    root().acceptVisitor(OpVisitor.traverse(symbols::bindSymbol));
    return symbols;
  }
}
