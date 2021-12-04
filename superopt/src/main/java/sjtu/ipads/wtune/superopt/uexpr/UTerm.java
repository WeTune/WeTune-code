package sjtu.ipads.wtune.superopt.uexpr;

import java.util.List;

/** A U-expr. */
public interface UTerm {
  String FUNC_IS_NULL_NAME = "IsNull";

  UKind kind();

  List<UTerm> subTerms();

  UTerm replaceBaseVar(UVar baseVar, UVar repVar);
}
