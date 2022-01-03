package sjtu.ipads.wtune.sql.ast;

import sjtu.ipads.wtune.sql.ast.constants.Category;
import sjtu.ipads.wtune.sql.ast.internal.SQLDataTypeImpl;

import java.util.List;

public interface SQLDataType {
  Category category();

  String name();

  int width();

  int precision();

  boolean unsigned();

  List<String> valuesList();

  boolean isArray();

  int storageSize();

  int[] dimensions();

  void formatAsDataType(StringBuilder builder, String dbType);

  void formatAsCastType(StringBuilder builder, String dbType);

  SQLDataType setUnsigned(boolean unsigned);

  SQLDataType setIntervalField(String intervalField);

  SQLDataType setValuesList(List<String> valuesList);

  SQLDataType setDimensions(int[] dimensions);

  static SQLDataType make(Category category, String name, int width, int precision) {
    return SQLDataTypeImpl.build(category, name, width, precision);
  }
}