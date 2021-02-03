package sjtu.ipads.wtune.superopt.util;

import sjtu.ipads.wtune.superopt.plan.*;

public class Stringify implements PlanVisitor {
  private final StringBuilder builder;
  private final boolean informative;

  public Stringify(boolean informative) {
    this.builder = new StringBuilder();
    this.informative = informative;
  }

  public static String stringify(Plan g) {
    final Stringify stringify = new Stringify(false);
    g.acceptVisitor(stringify);
    return stringify.toString();
  }

  public static String stringify(Plan g, boolean informative) {
    final Stringify stringify = new Stringify(informative);
    g.acceptVisitor(stringify);
    return stringify.toString();
  }

  @Override
  public boolean enter(PlanNode op) {
    builder.append(op);
    if (informative) attachInformation(op);
    if (!(op instanceof Input)) builder.append('(');
    return true;
  }

  @Override
  public void enterEmpty(PlanNode parent, int idx) {
    builder.append("\u25a1,");
  }

  @Override
  public void leave(PlanNode op) {
    if (builder.charAt(builder.length() - 1) == ',') builder.deleteCharAt(builder.length() - 1);
    if (!(op instanceof Input)) builder.append(')');
    builder.append(",");
  }

  private void attachInformation(PlanNode op) {
    builder.append('<');
    switch (op.type()) {
      case Input:
        builder.append(((Input) op).table());
        break;
      case InnerJoin:
      case LeftJoin:
        final Join join = (Join) op;
        builder.append(join.leftFields()).append(' ').append(join.rightFields());
        break;
      case PlainFilter:
        final PlainFilter plainFilter = (PlainFilter) op;
        builder.append(plainFilter.predicate()).append(' ').append(plainFilter.fields());
        break;
      case SubqueryFilter:
        builder.append(((SubqueryFilter) op).fields());
        break;
      case Proj:
        builder.append(((Proj) op).fields());
        break;
    }
    builder.append('>');
  }

  @Override
  public String toString() {
    if (builder.charAt(builder.length() - 1) == ',') builder.deleteCharAt(builder.length() - 1);
    return builder.toString();
  }
}
