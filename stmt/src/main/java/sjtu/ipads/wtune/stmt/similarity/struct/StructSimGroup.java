package sjtu.ipads.wtune.stmt.similarity.struct;

import sjtu.ipads.wtune.stmt.dao.StructGroupDao;
import sjtu.ipads.wtune.stmt.similarity.SimGroup;
import sjtu.ipads.wtune.stmt.similarity.output.OutputSimGroup;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.*;
import java.util.stream.Collectors;

public class StructSimGroup extends SimGroup {
  public StructSimGroup(Set<Statement> stmts) {
    super(stmts);
  }

  @Override
  public void save() {
    StructGroupDao.instance().save(this);
  }

  public static Builder builder() {
    return new Builder();
  }

  private static class Builder extends SimGroup.Builder {
    private final Map<StructFeature, Set<Statement>> groups = new HashMap();

    @Override
    public void add(Statement stmt) {
      groups.computeIfAbsent(StructFeature.extractFrom(stmt), ignored -> new HashSet<>()).add(stmt);
    }

    @Override
    public List<SimGroup> build() {
      final List<SimGroup> groups =
          this.groups.values().stream()
              .filter(it -> it.size() > 1)
              .map(StructSimGroup::new)
              .collect(Collectors.toList());
      for (int i = 0; i < groups.size(); i++) groups.get(i).setGroupId(i);
      return groups;
    }
  }
}
