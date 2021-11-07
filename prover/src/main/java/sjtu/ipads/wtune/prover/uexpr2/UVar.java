package sjtu.ipads.wtune.prover.uexpr2;

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

  UVar[] usedVars();

  VarKind kind();

  static UVar mkBase(UName name) {
    final UVar var = new UVarImpl(VarKind.BASE, name, new UVar[1]);
    var.usedVars()[0] = var;
    return var;
  }

  UName NAME_CONCAT = UName.mk("concat");
  UName NAME_EQ = UName.mk("eq");

  static UVar mkConcat(UVar v0, UVar v1) {
    return new UVarImpl(VarKind.CONCAT, NAME_CONCAT, arrayConcat(v0.usedVars(), v1.usedVars()));
  }

  static UVar mkProj(UName attrName, UVar v) {
    return new UVarImpl(VarKind.PROJ, attrName, new UVar[] {v});
  }

  static UVar mkFunc(UName funcName, UVar v) {
    return new UVarImpl(VarKind.FUNC, funcName, new UVar[] {v});
  }

  static UVar mkEq(UVar v0, UVar v1) {
    return new UVarImpl(VarKind.EQ, UVar.NAME_EQ, new UVar[] {v0, v1});
  }
}
