package sjtu.ipads.wtune.systhesis;

import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.ArrayList;
import java.util.List;

public class SynthesisOutput {
  public Statement base = null;
  public List<Statement> optimized = new ArrayList<>();

  public long baseP50 = -1;
  public List<Long> optP50 = new ArrayList<>();

  public int totalRefCount = 0;
  public int usedRefCount = 0;
  public int producedCount = 0;

  public long relationElapsed = 0;
  public long predicateElapsed = 0;
  public long exprListElapsed = 0;
  public long verificationElapsed = 0;

  public long totalElapsed() {
    return relationElapsed + predicateElapsed + exprListElapsed + verificationElapsed;
  }
}
