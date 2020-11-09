package sjtu.ipads.wtune.systhesis.op;

import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OpContext {
  private final Statement current;
  private final List<Op> ops;

  private OpContext(Statement current) {
    this(current, Collections.emptyList());
  }

  private OpContext(Statement current, List<Op> ops) {
    this.current = current;
    this.ops = ops;
  }

  public static OpContext build(Statement stmt) {
    return new OpContext(stmt);
  }

  public OpContext derive() {
    return new OpContext(current.copy(), new ArrayList<>(ops));
  }

  public Statement current() {
    return current;
  }

  public void addOp(Op op) {
    ops.add(op);
  }

  public List<Op> ops() {
    return ops;
  }

  @Override
  public String toString() {
    return ops + " => " + current.parsed();
  }
}
