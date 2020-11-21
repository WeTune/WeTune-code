package sjtu.ipads.wtune.superopt.impl;

import sjtu.ipads.wtune.superopt.GraphVisitor;
import sjtu.ipads.wtune.superopt.Operator;
import sjtu.ipads.wtune.superopt.operators.Input;

class Stringifier implements GraphVisitor {
  private final StringBuilder builder = new StringBuilder();

  @Override
  public boolean enter(Operator op) {
    builder.append(op);
    if (!(op instanceof Input)) builder.append('(');
    return true;
  }

  @Override
  public void enterEmpty(Operator parent, int idx) {
    builder.append("\u25a1,");
  }

  @Override
  public void leave(Operator op) {
    if (builder.charAt(builder.length() - 1) == ',') builder.deleteCharAt(builder.length() - 1);
    if (!(op instanceof Input)) builder.append(')');
    builder.append(",");
  }

  @Override
  public String toString() {
    return builder.toString();
  }
}
