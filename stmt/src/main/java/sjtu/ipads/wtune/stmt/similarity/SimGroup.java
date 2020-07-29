package sjtu.ipads.wtune.stmt.similarity;

import sjtu.ipads.wtune.stmt.similarity.output.OutputSimGroup;
import sjtu.ipads.wtune.stmt.similarity.struct.StructSimGroup;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class SimGroup {
  private int groupId;
  private Set<Statement> stmts;

  public SimGroup() {}

  public SimGroup(Set<Statement> stmts) {
    this.stmts = stmts;
  }

  public int groupId() {
    return groupId;
  }

  public Set<Statement> stmts() {
    return stmts;
  }

  public void setGroupId(int groupId) {
    this.groupId = groupId;
  }

  public void setStmts(Set<Statement> stmts) {
    this.stmts = stmts;
  }

  public void save() {}

  public static Builder outputGroupBuilder() {
    return OutputSimGroup.builder();
  }

  public static Builder structGroupBuilder() {
    return StructSimGroup.builder();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SimGroup that = (SimGroup) o;
    return groupId == that.groupId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupId);
  }

  public abstract static class Builder {
    public abstract void add(Statement stmt);

    public abstract List<SimGroup> build();
  }
}
