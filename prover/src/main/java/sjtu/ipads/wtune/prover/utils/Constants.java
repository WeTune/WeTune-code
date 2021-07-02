package sjtu.ipads.wtune.prover.utils;

import sjtu.ipads.wtune.prover.expr.Tuple;

public class Constants {
  public static final String FREE_VAR = "t";
  public static final String TRANSLATOR_VAR_PREFIX = "t";
  public static final String NORMALIZATION_VAR_PREFIX = "a";
  public static final String DECISION_VAR_PREFIX = "x";
  public static final String EXTRA_VAR_PREFIX = "e";
  public static final String TEMP_VAR_PREFIX_0 = "m";
  public static final String TEMP_VAR_PREFIX_1 = "n";
  public static final Tuple NULL_TUPLE = Tuple.constant("null");
  public static final String NOT_NULL_PRED = "`?` not null";
}
