package sjtu.ipads.wtune.stmt.scriptgen;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class OutputImpl implements Output {
  private final PrintWriter writer;
  private final String indentString;
  private int indentLayer;

  private static final String DEFAULT_INDENT = "  ";
  private static final List<String> DEFAULT_INDENT_CACHE = makeCache(DEFAULT_INDENT);

  private final List<String> indentCache;

  private static List<String> makeCache(String indent) {
    final List<String> cache = new ArrayList<>(10);
    for (int i = 0; i < 10; i++) cache.add(indent.repeat(i));
    return cache;
  }

  public OutputImpl(PrintWriter writer) {
    this(writer, "  ");
  }

  public OutputImpl(PrintWriter writer, String indentString) {
    this.writer = writer;
    this.indentLayer = 0;
    this.indentString = indentString;
    if (indentString.equals(DEFAULT_INDENT)) indentCache = DEFAULT_INDENT_CACHE;
    else indentCache = makeCache(indentString);
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
    writer.print(currentIndent());
    return this;
  }

  @Override
  public String currentIndent() {
    if (indentLayer < 10) return indentCache.get(indentLayer);
    else return indentString.repeat(indentLayer);
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
