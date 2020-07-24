package sjtu.ipads.wtune.stmt.similarity.output;

import sjtu.ipads.wtune.stmt.dao.OutputGroupDao;
import sjtu.ipads.wtune.stmt.statement.OutputFingerprint;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.*;
import java.util.stream.Collectors;

import static sjtu.ipads.wtune.common.utils.Commons.safeGet;

public class OutputSimGroup {
  private int groupId;
  private Set<Statement> stmts;

  public OutputSimGroup() {}

  public OutputSimGroup(Set<Statement> stmts) {
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

  public void save() {
    OutputGroupDao.instance().save(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    OutputSimGroup that = (OutputSimGroup) o;
    return groupId == that.groupId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupId);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final Map<OutputSimKey, Set<Statement>> groups = new HashMap<>(1024);

    private Builder() {}

    public void add(Statement stmt) {
      for (OutputSimKey key : OutputFingerprint.extractKey(stmt))
        groups.computeIfAbsent(key, ignored -> new HashSet<>()).add(stmt);
    }

    public List<OutputSimGroup> build() {
      final List<OutputSimGroup> groups =
          this.groups.values().stream()
              .filter(it -> it.size() > 1)
              .distinct()
              .map(OutputSimGroup::new)
              .collect(Collectors.toList());
      for (int i = 0; i < groups.size(); i++) groups.get(i).setGroupId(i);
      return groups;
    }
  }
}
