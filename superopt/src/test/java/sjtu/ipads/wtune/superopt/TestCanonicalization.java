package sjtu.ipads.wtune.superopt;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.superopt.fragment.Fragment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.superopt.fragment.Fragment.wrap;
import static sjtu.ipads.wtune.superopt.fragment.Operator.plainFilter;
import static sjtu.ipads.wtune.superopt.fragment.Operator.subqueryFilter;
import static sjtu.ipads.wtune.superopt.internal.Canonicalization.canonicalize;

public class TestCanonicalization {
  @Test
  void test() {
    final Fragment fragment =
        canonicalize(wrap(subqueryFilter(plainFilter(plainFilter(null)), null)).setup());
    assertEquals("PlainFilter(PlainFilter(SubqueryFilter(Input,Input)))", fragment.toString());
  }
}
