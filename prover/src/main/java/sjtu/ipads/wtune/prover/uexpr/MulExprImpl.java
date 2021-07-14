package sjtu.ipads.wtune.prover.uexpr;

final class MulExprImpl extends BinaryExpr implements MulExpr {
  @Override
  protected UExprBase copy0() {
    return new MulExprImpl();
  }

  @Override
  public StringBuilder stringify(StringBuilder builder) {
    final UExpr c0 = children[0];
    final UExpr c1 = children[1];

    final boolean needParen0 = c0.kind() == Kind.ADD;
    if (needParen0) builder.append('(');
    c0.stringify(builder);
    if (needParen0) builder.append(')');

    builder.append(' ').append('*').append(' ');

    final boolean needParen1 = c1.kind() == Kind.ADD || c1.kind() == Kind.MUL;
    if (needParen1) builder.append('(');
    c1.stringify(builder);
    if (needParen1) builder.append(')');

    return builder;
  }
}
