package sjtu.ipads.wtune.superopt.uexpr;

import static sjtu.ipads.wtune.common.utils.Commons.arrayConcat;

public interface UVar {
  enum VarKind {
    BASE,
    CONCAT,
    PROJ,
    FUNC,
    EQ
  }

  UVar replaceBaseVar(UVar baseVar, UVar repVar);

  UName name();

  UVar[] argument();

  VarKind kind();

  static UVar mkBase(UName name) {
    final UVar var = new UVarImpl(VarKind.BASE, name, new UVar[1]);
    var.argument()[0] = var;
    return var;
  }

  UName NAME_CONCAT = UName.mk("concat");
  UName NAME_EQ = UName.mk("eq");

  static UVar mkConcat(UVar v0, UVar v1) {
    assert v0.kind() == VarKind.BASE || v0.kind() == VarKind.CONCAT;
    assert v1.kind() == VarKind.BASE || v1.kind() == VarKind.CONCAT;
    return new UVarImpl(VarKind.CONCAT, NAME_CONCAT, arrayConcat(v0.argument(), v1.argument()));
  }

  static UVar mkProj(UName attrName, UVar v) {
    assert v.kind() == VarKind.BASE || v.kind() == VarKind.CONCAT;
    return new UVarImpl(VarKind.PROJ, attrName, new UVar[] {v});
  }

  static UVar mkFunc(UName funcName, UVar v) {
    assert v.kind() == VarKind.BASE || v.kind() == VarKind.CONCAT;
    return new UVarImpl(VarKind.FUNC, funcName, new UVar[] {v});
  }

  static UVar mkEq(UVar v0, UVar v1) {
    assert v0.kind() != VarKind.FUNC && v0.kind() != VarKind.EQ;
    assert v1.kind() != VarKind.FUNC && v1.kind() != VarKind.EQ;
    return new UVarImpl(VarKind.EQ, UVar.NAME_EQ, new UVar[] {v0, v1});
  }
}
