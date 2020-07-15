package sjtu.ipads.wtune.stmt.scriptgen;

import java.io.PrintWriter;

public class OutputImpl implements Output {
  private final PrintWriter writer;
  private final String indentString;
  private int indentLayer;

  public OutputImpl(PrintWriter writer) {
    this(writer, "  ");
  }

  public OutputImpl(PrintWriter writer, String indentString) {
    this.writer = writer;
    this.indentLayer = 0;
    this.indentString = indentString;
  }

  @Override
  public Output increaseIndent() {
    ++indentLayer;
    return this;
  }

  @Override
  public Output decreaseIndent() {
    indentLayer = Math.max(0, indentLayer - 1);
    return this;
  }

  @Override
  public Output indent() {
    writer.print(indentString.repeat(indentLayer));
    return this;
  }

  @Override
  public Output println() {
    writer.println();
    return this;
  }

  @Override
  public Output println(String str) {
    writer.println(str);
    return this;
  }

  @Override
  public Output print(String str) {
    writer.print(str);
    return this;
  }

  @Override
  public Output printf(String str, Object... args) {
    writer.printf(str, args);
    return this;
  }
}
