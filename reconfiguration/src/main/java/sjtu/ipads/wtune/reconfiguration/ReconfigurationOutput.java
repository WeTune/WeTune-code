package sjtu.ipads.wtune.reconfiguration;

import sjtu.ipads.wtune.stmt.schema.SchemaPatch;
import sjtu.ipads.wtune.stmt.schema.Table;

import java.util.*;
import java.util.stream.Collectors;

public class ReconfigurationOutput {
  public String appName;

  public final Set<Index> indexHints = new HashSet<>();
  public final Map<Table, String> engineHints = new HashMap<>();

  public List<SchemaPatch> patches() {
    return indexHints.stream()
        .map(Index::toPatch)
        .peek(it -> it.setApp(appName))
        .collect(Collectors.toList());
  }
}
