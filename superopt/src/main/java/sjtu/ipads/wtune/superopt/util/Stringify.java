package sjtu.ipads.wtune.superopt.util;

import sjtu.ipads.wtune.superopt.fragment.*;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Numbering;
import sjtu.ipads.wtune.superopt.fragment.symbolic.Placeholder;

public class Stringify implements OperatorVisitor {
  private final StringBuilder builder;
  private final Numbering numbering;

  public Stringify(Numbering numbering) {
    this.builder = new StringBuilder();
    this.numbering = numbering;
  }

  public static String stringify(Fragment g) {
    final Stringify stringify = new Stringify(null);
    g.acceptVisitor(stringify);
    return stringify.toString();
  }

  public static String stringify(Fragment g, Numbering numbering) {
    final Stringify stringify = new Stringify(numbering);
    g.acceptVisitor(stringify);
    return stringify.toString();
  }

  @Override
  public boolean enter(Operator op) {
    builder.append(op);
    attachInformation(op);
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

  private void attachInformation(Operator op) {
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
