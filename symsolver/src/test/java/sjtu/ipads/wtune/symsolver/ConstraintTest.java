package sjtu.ipads.wtune.symsolver;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.symsolver.core.Constraint;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.Sym;
import sjtu.ipads.wtune.symsolver.core.TableSym;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.common.utils.FuncUtils.generate;
import static sjtu.ipads.wtune.symsolver.core.Constraint.*;
import static sjtu.ipads.wtune.symsolver.utils.Indexed.number;

public class ConstraintTest {
  @Test
  void test() {
    final TableSym[] tbls = generate(3, TableSym.class, i -> TableSym.from(null, i));
    final PickSym[] picks = generate(3, PickSym.class, i -> PickSym.from(null, i));

    final Class<IllegalArgumentException> wrongArgEx = IllegalArgumentException.class;
    assertThrows(wrongArgEx, () -> tableEq(tbls[0], tbls[0]));
    assertThrows(wrongArgEx, () -> tableEq(tbls[0], tbls[1]));
    assertThrows(wrongArgEx, () -> pickEq(picks[0], picks[0]));
    assertThrows(wrongArgEx, () -> pickEq(picks[0], picks[1]));
    assertThrows(wrongArgEx, () -> pickFrom(picks[0], tbls[0]));
    assertThrows(wrongArgEx, () -> reference(tbls[0], picks[0], tbls[0], picks[0]));
    assertThrows(wrongArgEx, () -> reference(tbls[0], picks[0], tbls[1], picks[1]));

    number(tbls, 0);
    number(picks, 0);

    assertThrows(wrongArgEx, () -> tableEq(tbls[0], tbls[0]));
    assertThrows(wrongArgEx, () -> pickEq(picks[0], picks[0]));
    assertThrows(wrongArgEx, () -> reference(tbls[0], picks[0], tbls[0], picks[0]));

    final Constraint tblEq0 = tableEq(tbls[1], tbls[0]), tblEq1 = tableEq(tbls[2], tbls[0]);
    final Constraint pickEq0 = pickEq(picks[1], picks[0]), pickEq1 = pickEq(picks[2], picks[0]);
    final Constraint pickFrom0 = pickFrom(picks[0], tbls[0]),
        pickFrom1 = pickFrom(picks[1], tbls[1]);
    final Constraint ref0 = reference(tbls[0], picks[0], tbls[1], picks[1]),
        ref1 = reference(tbls[1], picks[1], tbls[0], picks[0]);

    assertArrayEquals(new Sym[] {tbls[0], tbls[1]}, tblEq0.targets());
    assertArrayEquals(new Sym[] {picks[0], picks[1]}, pickEq0.targets());
    assertArrayEquals(new Sym[] {picks[0], tbls[0]}, pickFrom0.targets());
    assertArrayEquals(new Sym[] {tbls[0], picks[0], tbls[1], picks[1]}, ref0.targets());

    final List<Constraint> constraints =
        Arrays.asList(ref1, ref0, pickFrom1, pickFrom0, pickEq1, pickEq0, tblEq1, tblEq0);
    final List<Constraint> sortedConstraint =
        Arrays.asList(tblEq0, tblEq1, pickEq0, pickEq1, pickFrom0, pickFrom1, ref0, ref1);

    constraints.sort(Constraint::compareTo);
    assertEquals(sortedConstraint, constraints);
  }
}
