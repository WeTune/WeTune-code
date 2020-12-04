package sjtu.ipads.wtune.solver.sql.expr;

import sjtu.ipads.wtune.solver.node.AlgNode;
import sjtu.ipads.wtune.solver.sql.ColumnRef;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

public interface NameInputRef extends InputRef {
  String name();

  @Override
  default IndexInputRef locateIn(List<AlgNode> inputs, List<String> aliases) {
    final String name = name();
    final int dotIdx = name.indexOf('.');
    final String targetInputAlias = dotIdx == -1 ? null : name.substring(0, dotIdx);
    final String columnName = name.substring(dotIdx + 1);

    for (int i = 0, baseIdx = 0; i < inputs.size(); i++) {
      final List<ColumnRef> columns = inputs.get(i).columns();

      if (targetInputAlias != null) {
        if (targetInputAlias.equals(aliases.get(i))) {
          final int offset = findColumn(columnName, columns);
          if (offset != -1) return InputRef.ref(baseIdx + offset);
          break;
        }

      } else {
        final int offset = findColumn(columnName, columns);
        if (offset != -1) return InputRef.ref(baseIdx + offset);
      }

      baseIdx += columns.size();
    }

    throw new NoSuchElementException("cannot found input '" + name + "' in " + inputs);
  }

  private static int findColumn(String name, List<ColumnRef> cols) {
    for (int i = 0; i < cols.size(); i++) if (Objects.equals(name, cols.get(i).alias())) return i;
    return -1;
  }

  @Override
  default Expr compile(List<AlgNode> inputs, List<String> alias) {
    return locateIn(inputs, alias);
  }
}
