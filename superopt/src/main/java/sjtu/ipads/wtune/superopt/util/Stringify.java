package sjtu.ipads.wtune.superopt.util;

import sjtu.ipads.wtune.superopt.plan.*;

public class Stringify implements PlanVisitor {
  private final StringBuilder builder;
  private final PlaceholderNumbering numbering;

  public Stringify(PlaceholderNumbering numbering) {
    this.builder = new StringBuilder();
    this.numbering = numbering;
  }

  public static String stringify(Plan g) {
    final Stringify stringify = new Stringify(null);
    g.acceptVisitor(stringify);
    return stringify.toString();
  }

  public static String stringify(Plan g, PlaceholderNumbering numbering) {
    final Stringify stringify = new Stringify(numbering);
    g.acceptVisitor(stringify);
    return stringify.toString();
  }

  @Override
  public boolean enter(PlanNode op) {
    builder.append(op);
    attachInformation(op);
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
    if (numbering == null) return;

    builder.append('<');
    switch (op.type()) {
      case Input:
        builder.append(toString(((Input) op).table()));
        break;
      case InnerJoin:
      case LeftJoin:
        final Join join = (Join) op;
        builder
            .append(toString(join.leftFields()))
            .append(' ')
            .append(toString(join.rightFields()));
        break;
      case PlainFilter:
        final PlainFilter plainFilter = (PlainFilter) op;
        builder
            .append(toString(plainFilter.predicate()))
            .append(' ')
            .append(toString(plainFilter.fields()));
        break;
      case SubqueryFilter:
        builder.append(toString(((SubqueryFilter) op).fields()));
        break;
      case Proj:
        builder.append(toString(((Proj) op).fields()));
        break;
    }
    builder.append('>');
  }

  private String toString(Placeholder placeholder) {
    return placeholder.tag() + numbering.numberOf(placeholder);
  }

  @Override
  public String toString() {
    if (builder.charAt(builder.length() - 1) == ',') builder.deleteCharAt(builder.length() - 1);
    return builder.toString();
  }
}
