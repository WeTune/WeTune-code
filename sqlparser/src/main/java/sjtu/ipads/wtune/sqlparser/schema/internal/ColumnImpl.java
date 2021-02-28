package sjtu.ipads.wtune.sqlparser.schema.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.ast.constants.Category;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Constraint;
import sjtu.ipads.wtune.sqlparser.schema.SchemaPatch;

import java.util.*;

import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.schema.Column.Flag.*;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.simpleName;

public class ColumnImpl implements Column {
  private final String table;
  private final String name;
  private final String rawDataType;
  private final SQLDataType dataType;
  private final EnumSet<Flag> flags;
  private List<Constraint> constraints;
  private List<SchemaPatch> patches;

  private ColumnImpl(String table, String name, String rawDataType, SQLDataType dataType) {
    this.table = table;
    this.name = name;
    this.rawDataType = rawDataType;
    this.dataType = dataType;
    this.flags = EnumSet.noneOf(Flag.class);

    if (dataType.category() == Category.BOOLEAN) flags.add(IS_BOOLEAN);
    else if (dataType.category() == Category.ENUM) flags.add(IS_ENUM);
  }

  public static ColumnImpl build(String table, ASTNode colDef) {
    final String colName = simpleName(colDef.get(COLUMN_DEF_NAME).get(COLUMN_NAME_COLUMN));
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
  public String tableName() {
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
  public Collection<SchemaPatch> patches() {
    return patches == null ? Collections.emptyList() : patches;
  }

  @Override
  public boolean isFlag(Flag flag) {
    return flags.contains(flag);
  }

  void addConstraint(Constraint constraint) {
    if (constraints == null) constraints = new ArrayList<>(3);
    constraints.add(constraint);
    if (constraint.type() == null) flags.add(INDEXED);
    else
      switch (constraint.type()) {
        case PRIMARY:
          flags.add(PRIMARY);
        case UNIQUE:
          flags.add(UNIQUE);
          flags.add(INDEXED);
          break;
        case NOT_NULL:
          flags.add(NOT_NULL);
          break;
        case FOREIGN:
          flags.add(FOREIGN_KEY);
          flags.add(INDEXED);
          break;
        case CHECK:
          flags.add(HAS_CHECK);
          break;
      }
  }

  void addPatch(SchemaPatch patch) {
    if (patches == null) patches = new ArrayList<>(2);
    patches.add(patch);
    switch (patch.type()) {
      case INDEX -> flags.add(INDEXED);
      case BOOLEAN -> flags.add(IS_BOOLEAN);
      case ENUM -> flags.add(IS_ENUM);
      case UNIQUE -> flags.add(UNIQUE);
      case FOREIGN_KEY -> flags.add(FOREIGN_KEY);
    }
  }

  @Override public String toString() {
    return table + "." + name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ColumnImpl column = (ColumnImpl) o;
    return Objects.equals(table, column.table) && Objects.equals(name, column.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(table, name);
  }
}
