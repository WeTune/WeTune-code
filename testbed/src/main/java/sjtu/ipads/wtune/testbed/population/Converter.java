package sjtu.ipads.wtune.testbed.population;

import org.apache.commons.lang3.NotImplementedException;
import sjtu.ipads.wtune.sqlparser.ast1.constants.Category;
import sjtu.ipads.wtune.sqlparser.ast1.constants.DataTypeName;
import sjtu.ipads.wtune.sqlparser.ast1.SqlDataType;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Column.Flag;
import sjtu.ipads.wtune.testbed.common.BatchActuator;

public interface Converter extends Generator {
  void convert(int seed, BatchActuator actuator);

  @Override
  default void generate(int seed, BatchActuator actuator) {
    convert(seed, actuator);
  }

  static Converter makeConverter(Column column) {
    final SqlDataType dataType = column.dataType();
    if (column.isFlag(Flag.IS_BOOLEAN)
        && dataType.category() != Category.BOOLEAN
        && dataType.category() != Category.BIT_STRING) {
      return new EffectiveEnumConverter(2, dataType);
    }

    if (column.isFlag(Flag.IS_ENUM) && dataType.category() != Category.ENUM)
      return new EffectiveEnumConverter(10, dataType);

    switch (dataType.category()) {
      case INTEGRAL:
        return new IntegralConverter(dataType);
      case FRACTION:
        return new FractionConverter(dataType);
      case BOOLEAN:
        return new BooleanConverter(dataType);
      case ENUM:
        return new EnumConverter(dataType);
      case STRING:
        return new StringConverter(dataType);
      case BIT_STRING:
        if (dataType.width() == 1) return new BooleanConverter(dataType);
        else throw new NotImplementedException();
      case TIME:
        return new TimeConverter(dataType);
      case BLOB:
        return new BlobConverter(dataType);
      case JSON:
        return new JsonConverter(dataType);
      case NET:
        return new NetConverter(dataType);
      case UUID:
        return new UuidConverter(dataType);
      case UNCLASSIFIED:
        if (DataTypeName.TSVECTOR.equals(dataType.name())) return new TsVectorConverter(dataType);
      default:
        if ("bytea".equals(dataType.name())) return new ByteaConverter(dataType);

        throw new NotImplementedException();
    }
  }
}
