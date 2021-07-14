package sjtu.ipads.wtune.prover.uexpr;

final class AddExprImpl extends BinaryExpr implements AddExpr {
  @Override
  protected UExprBase copy0() {
    return new AddExprImpl();
  }

  @Override
  public StringBuilder stringify(StringBuilder builder) {
    children[0].stringify(builder).append(' ').append('+').append(' ');

    final boolean needParen = children[1].kind() == Kind.ADD;
    if (needParen) builder.append('(');
    children[1].stringify(builder);
    if (needParen) builder.append(')');

    return builder;
  }
}
