package sjtu.ipads.wtune.superopt.fragment.internal;

import static sjtu.ipads.wtune.superopt.util.Stringify.stringify;

import java.util.ArrayDeque;
import java.util.Deque;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;
import sjtu.ipads.wtune.superopt.fragment.Fragment;
import sjtu.ipads.wtune.superopt.fragment.Input;
import sjtu.ipads.wtune.superopt.fragment.Operator;
import sjtu.ipads.wtune.superopt.fragment.OperatorVisitor;
import sjtu.ipads.wtune.superopt.fragment.Operators;
import sjtu.ipads.wtune.superopt.fragment.Semantic;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholders;
import sjtu.ipads.wtune.superopt.fragment.symbolic.internal.PlaceholdersImpl;
import sjtu.ipads.wtune.superopt.util.Hole;

public class FragmentImpl implements Fragment {
  private int id;
  public Operator head;
  private boolean alreadySetup;

  private final Placeholders placeholders;

  private FragmentImpl() {
    placeholders = new PlaceholdersImpl();
  }

  public static Fragment build() {
    return new FragmentImpl();
  }

  public static Fragment build(String str) {
    final String[] opStrs = str.split("[(),]+");
    final Deque<Operator> operators = new ArrayDeque<>(opStrs.length);

    for (int i = opStrs.length - 1; i >= 0; i--) {
      final String opStr = opStrs[i];
      final OperatorType type = OperatorType.valueOf(opStr.split("[<> ]+", 2)[0]);
      final Operator op = Operators.create(type);

      for (int j = 0; j < type.numPredecessors(); j++) op.setPredecessor(j, operators.pop());

      operators.push(op);
    }

    return Fragment.wrap(operators.pop()).setup();
  }

  @Override
  public void setId(int id) {
    this.id = id;
  }

  @Override
  public int id() {
    return id;
  }

  @Override
  public Operator head() {
    return head;
  }

  @Override
  public Fragment setup() {
    if (alreadySetup) return this;
    alreadySetup = true;

    for (Hole<Operator> hole : holes()) hole.fill(Input.create());

    acceptVisitor(OperatorVisitor.traverse(it -> it.setFragment(this)));

    return this;
  }

  @Override
  public void setHead(Operator head) {
    this.head = head;
  }

  @Override
  public String toString() {
    return stringify(this);
  }

  @Override
  public Semantic semantic() {
    setup();
    return Semantic.build(this);
  }

  @Override
  public Placeholders placeholders() {
    return placeholders;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Fragment)) return false;
    final Fragment fragment = (Fragment) o;
    if ((this.head == null) != (fragment.head() == null)) return false;
    if (this.head == null) return true;
    return this.head.structuralEquals(fragment.head());
  }

  @Override
  public int hashCode() {
    return head == null ? 0 : head.structuralHash();
  }
}
