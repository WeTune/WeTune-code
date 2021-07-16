package sjtu.ipads.wtune.prover.logic;

interface LogicObj {
  Object underlying();

  LogicCtx ctx();

  <T> T unwrap(Class<T> cls);
}
