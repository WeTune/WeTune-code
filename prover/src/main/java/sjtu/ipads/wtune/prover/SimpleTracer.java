package sjtu.ipads.wtune.prover;

import java.util.ArrayList;
import java.util.List;

import static sjtu.ipads.wtune.common.utils.Commons.joining;

public class SimpleTracer implements Tracer {
  private final List<String> operations = new ArrayList<>();
  private String prologue, epilogue;

  @Override
  public void trace(String operation) {
    operations.add(operation);
  }

  @Override
  public void setEpilogue(String epilogue) {
    this.epilogue = epilogue;
  }

  @Override
  public void setPrologue(String prologue) {
    this.prologue = prologue;
  }

  @Override
  public String getTrace() {
    final StringBuilder builder = new StringBuilder();
    if (prologue != null) builder.append(prologue);
    joining(",\n", operations, builder);
    if (epilogue != null) builder.append(epilogue);

    return builder.toString();
  }
}
