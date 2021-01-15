package sjtu.ipads.wtune.sqlparser.rel.internal;

import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.ast.SQLNode;
import sjtu.ipads.wtune.sqlparser.rel.Column;
import sjtu.ipads.wtune.sqlparser.rel.Constraint;

import java.util.*;

import static sjtu.ipads.wtune.sqlparser.ast.NodeAttrs.*;
import static sjtu.ipads.wtune.sqlparser.rel.Column.Flag.*;

public class ColumnImpl implements Column {
  private final String table;
  private final String name;
  private final String rawDataType;
  private final SQLDataType dataType;
  private final EnumSet<Flag> flags;
  private List<Constraint> constraints;

  private ColumnImpl(String table, String name, String rawDataType, SQLDataType dataType) {
    this.table = table;
    this.name = name;
    this.rawDataType = rawDataType;
    this.dataType = dataType;
    this.flags = EnumSet.noneOf(Flag.class);
  }

  public static ColumnImpl build(String table, SQLNode colDef) {
    final String colName = colDef.get(COLUMN_DEF_NAME).get(COLUMN_NAME_COLUMN);
    final String rawDataType = colDef.get(COLUMN_DEF_DATATYPE_RAW);
    final SQLDataType dataType = colDef.get(COLUMN_DEF_DATATYPE);

    final ColumnImpl column = new ColumnImpl(table, colName, rawDataType, dataType);

    if (colDef.isFlag(COLUMN_DEF_GENERATED)) column.flag(GENERATED);
    if (colDef.isFlag(COLUMN_DEF_DEFAULT)) column.flag(HAS_DEFAULT);
    if (colDef.isFlag(COLUMN_DEF_AUTOINCREMENT)) column.flag(AUTO_INCREMENT);

    return column;
  }

  void flag(Flag... flags) {
    this.flags.addAll(Arrays.asList(flags));
  }

  @Override
  public String table() {
    return table;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String rawDataType() {
    return rawDataType;
  }

  @Override
  public SQLDataType dataType() {
    return dataType;
  }

  @Override
  public Collection<Constraint> constraints() {
    return constraints == null ? Collections.emptyList() : constraints;
  }

  @Override
  public boolean isFlagged(Flag flag) {
    return flags.contains(flag);
  }

  void addConstraint(Constraint constraint) {
    if (constraints == null) constraints = new ArrayList<>(3);
    constraints.add(constraint);
  }
}
