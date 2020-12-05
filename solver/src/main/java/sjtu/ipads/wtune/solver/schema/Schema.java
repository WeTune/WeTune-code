package sjtu.ipads.wtune.solver.schema;

import sjtu.ipads.wtune.solver.schema.impl.SchemaImpl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.Collections.singletonList;

public interface Schema {
  Table table(String name);

  Collection<Table> tables();

  Map<List<Column>, List<Column>> foreignKeys();

  static Builder builder() {
    return SchemaImpl.builder();
  }

  interface Builder {
    Builder table(Function<Table.Builder, Table.Builder> builder);

    Builder foreignKey(List<String> referee, List<String> referred);

    Schema build();

    default Builder foreignKey(String referee, String referred) {
      return foreignKey(singletonList(referee), singletonList(referred));
    }
  }
}
