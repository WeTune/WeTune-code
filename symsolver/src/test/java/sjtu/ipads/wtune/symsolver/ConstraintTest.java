package sjtu.ipads.wtune.symsolver;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.symsolver.core.Indexed;
import sjtu.ipads.wtune.symsolver.core.PickSym;
import sjtu.ipads.wtune.symsolver.core.TableSym;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static sjtu.ipads.wtune.common.utils.FuncUtils.generate;
import static sjtu.ipads.wtune.symsolver.core.Indexed.number;

public class ConstraintTest {
  @Test
  void test() {
    final TableSym[] tbls = generate(3, TableSym.class, i -> TableSym.of());
    final PickSym[] picks = generate(3, PickSym.class, i -> PickSym.of());

    final Class<IllegalArgumentException> wrongArgEx = IllegalArgumentException.class;
    assertThrows(wrongArgEx, () -> DecidableConstraint.tableEq(tbls[0], tbls[0]));
    assertThrows(wrongArgEx, () -> DecidableConstraint.tableEq(tbls[0], tbls[1]));
    assertThrows(wrongArgEx, () -> DecidableConstraint.pickEq(picks[0], picks[0]));
    assertThrows(wrongArgEx, () -> DecidableConstraint.pickEq(picks[0], picks[1]));
    assertThrows(wrongArgEx, () -> DecidableConstraint.pickFrom(picks[0], tbls[0]));
    assertThrows(
        wrongArgEx, () -> DecidableConstraint.reference(tbls[0], picks[0], tbls[0], picks[0]));
    assertThrows(
        wrongArgEx, () -> DecidableConstraint.reference(tbls[0], picks[0], tbls[1], picks[1]));

    number(tbls, 0);
    number(picks, 0);

    assertThrows(wrongArgEx, () -> DecidableConstraint.tableEq(tbls[0], tbls[0]));
    assertThrows(wrongArgEx, () -> DecidableConstraint.pickEq(picks[0], picks[0]));
    assertThrows(
        wrongArgEx, () -> DecidableConstraint.reference(tbls[0], picks[0], tbls[0], picks[0]));

    final DecidableConstraint tblEq0 = DecidableConstraint.tableEq(tbls[1], tbls[0]),
        tblEq1 = DecidableConstraint.tableEq(tbls[2], tbls[0]);
    final DecidableConstraint pickEq0 = DecidableConstraint.pickEq(picks[1], picks[0]),
        pickEq1 = DecidableConstraint.pickEq(picks[2], picks[0]);
    final DecidableConstraint pickFrom0 = DecidableConstraint.pickFrom(picks[0], tbls[0]),
        pickFrom1 = DecidableConstraint.pickFrom(picks[1], tbls[1]);
    final DecidableConstraint
        ref0 = DecidableConstraint.reference(tbls[0], picks[0], tbls[1], picks[1]),
        ref1 = DecidableConstraint.reference(tbls[1], picks[1], tbls[0], picks[0]);

    assertArrayEquals(new Indexed[] {tbls[0], tbls[1]}, tblEq0.targets());
    assertArrayEquals(new Indexed[] {picks[0], picks[1]}, pickEq0.targets());
    assertArrayEquals(new Indexed[] {picks[0], tbls[0]}, pickFrom0.targets());
    assertArrayEquals(new Indexed[] {tbls[0], picks[0], tbls[1], picks[1]}, ref0.targets());

    final List<DecidableConstraint> constraints =
        Arrays.asList(ref1, ref0, pickFrom1, pickFrom0, pickEq1, pickEq0, tblEq1, tblEq0);
    final List<DecidableConstraint> sortedConstraint =
        Arrays.asList(tblEq0, tblEq1, pickEq0, pickEq1, ref0, ref1, pickFrom0, pickFrom1);

    constraints.sort(DecidableConstraint::compareTo);
    assertEquals(sortedConstraint, constraints);
  }
}