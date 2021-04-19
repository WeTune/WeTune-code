package sjtu.ipads.wtune.testbed.population;

import static java.lang.Integer.numberOfLeadingZeros;
import static sjtu.ipads.wtune.sqlparser.ast.constants.DataTypeName.DECIMAL;
import static sjtu.ipads.wtune.sqlparser.ast.constants.DataTypeName.DOUBLE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.DataTypeName.FIXED;
import static sjtu.ipads.wtune.sqlparser.ast.constants.DataTypeName.FLOAT;
import static sjtu.ipads.wtune.sqlparser.ast.constants.DataTypeName.NUMERIC;
import static sjtu.ipads.wtune.testbed.util.MathHelper.base10;
import static sjtu.ipads.wtune.testbed.util.MathHelper.isPow10;
import static sjtu.ipads.wtune.testbed.util.MathHelper.isPow2;
import static sjtu.ipads.wtune.testbed.util.RandomHelper.GLOBAL_SEED;

import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import sjtu.ipads.wtune.sqlparser.ast.SQLDataType;
import sjtu.ipads.wtune.sqlparser.ast.constants.Category;
import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;
import sjtu.ipads.wtune.sqlparser.schema.Column;
import sjtu.ipads.wtune.sqlparser.schema.Column.Flag;
import sjtu.ipads.wtune.sqlparser.schema.Constraint;
import sjtu.ipads.wtune.testbed.common.Element;

class SQLGenerators implements Generators {
  private final Config config;
  private final Map<Element, Generator> generators;

  SQLGenerators(Config config) {
    this.config = config;
    this.generators = new HashMap<>();
  }

  @Override
  public Generator bind(Element element) {
    return generators.computeIfAbsent(element, this::makeGenerator);
  }

  private Generator makeGenerator(Element element) {
    final Column column = element.unwrap(Column.class);

    if (column.isFlag(Flag.AUTO_INCREMENT)) return new AutoIncrementModifier();

    final Column referred = getReferencedColumn(column);
    final Constraint uk = getUniqueKey(column);

    Generator generator;
    if (referred == null) generator = Converter.makeConverter(column.dataType());
    else {
      generator = makeGenerator(Element.ofColumn(referred));
      generator = new ForeignKeyModifier(generator, config.getUnitCount(referred.tableName()));
    }

    if (uk == null)
      generator =
          new RandomModifier(
              column, config.getRandomGen(column.tableName(), column.name()), generator);
    else {
      int totalDigits = 0, totalBits = 0;
      boolean useDigits = true;
      int start = -1, end = -1;
      for (Column component : uk.columns()) {
        final int digits = getRequiredDigits(component);
        final int bits = getRequiredBits(component);
        assert bits >= 0 ^ digits >= 0;
        if (digits > 0) totalDigits += digits;
        if (bits > 0) totalBits += bits;
        if (component == column) {
          useDigits = digits >= 0;
          start = useDigits ? (totalDigits - digits) : (totalBits - bits);
          end = useDigits ? totalDigits : totalBits;
        }
      }

      assert start >= 0 && end >= start;
      final String ukStr =
          uk.columns().stream().map(Object::toString).collect(Collectors.joining());
      final int sharedLocalSeed = GLOBAL_SEED + ukStr.hashCode();

      if (useDigits) {
        final int[] adjusted = adjustRange(totalDigits, start, end, 9);
        totalDigits = adjusted[0];
        start = adjusted[1];
        end = adjusted[2];
        generator = new UniqueKeyModifierDec(sharedLocalSeed, totalDigits, start, end, generator);
      } else {
        final int[] adjusted = adjustRange(totalBits, start, end, 30);
        totalBits = adjusted[0];
        start = adjusted[1];
        end = adjusted[2];
        generator = new UniqueKeyModifierBin(sharedLocalSeed, totalBits, start, end, generator);
      }
    }

    return generator;
  }

  private static Column getReferencedColumn(Column column) {
    final Collection<Constraint> fks = column.constraints(ConstraintType.FOREIGN);
    if (fks.isEmpty()) return null;

    final Constraint fk = Iterables.get(fks, 0);
    return fk.refColumns().get(fk.columns().indexOf(column));
  }

  private static Constraint getUniqueKey(Column column) {
    final Collection<Constraint> pks = column.constraints(ConstraintType.PRIMARY);
    final Constraint pk = pks.isEmpty() ? null : Iterables.get(pks, 0);

    Constraint shortestUk = pk;
    final Collection<Constraint> uks = column.constraints(ConstraintType.UNIQUE);
    for (Constraint uk : uks)
      if (shortestUk == null || uk.columns().size() < shortestUk.columns().size()) shortestUk = uk;

    return shortestUk;
  }

  private int getRequiredBits(Column column) {
    final Column referenced = getReferencedColumn(column);

    // for foreign key, returns bits of the row count of referenced table's size
    // (if it is power of 2)
    if (referenced != null) {
      final int rowCount = config.getUnitCount(referenced.tableName());
      if (isPow2(rowCount)) return 32 - numberOfLeadingZeros(rowCount);
      else return -1;
    }

    final SQLDataType dataType = column.dataType();
    // otherwise, returns the bits that is required by the data type
    switch (dataType.category()) {
      case INTEGRAL:
        return dataType.storageSize() << 3; // bits = bytes * 8

      case FRACTION:
        if (DOUBLE.equals(dataType.name()) || FLOAT.equals(dataType.name())) return 32;
        else return -1; // decimal type (DECIMAL,NUMERIC,FIXED) should use digits

      case BOOLEAN:
        return 1;

      case ENUM:
        final int cardinality = dataType.valuesList().size();
        assert cardinality > 0;
        return 32 - numberOfLeadingZeros(cardinality - 1);

      default:
        return 32;
    }
  }

  private int getRequiredDigits(Column column) {
    final Column referenced = getReferencedColumn(column);
    if (referenced != null) {
      final int rowCount = config.getUnitCount(referenced.tableName());
      if (isPow10(rowCount)) return base10(rowCount);
      else return -1;
    }

    final SQLDataType dataType = column.dataType();
    if (dataType.category() == Category.FRACTION)
      switch (dataType.name()) {
        case FIXED:
        case NUMERIC:
        case DECIMAL:
          return dataType.width() + dataType.precision();
      }

    return -1;
  }

  private int[] adjustRange(int range, int start, int end, int limit) {
    if (range <= limit) return new int[] {range, start, end};

    final int width = Math.min(end - start, limit);
    final int rightPadding = range - end;
    final int adjustedRightPadding = (int) (rightPadding * (range / (double) limit));

    range = limit;
    start = Math.max(0, range - adjustedRightPadding - width);
    end = Math.min(range, start + width);

    return new int[] {range, start, end};
  }
}
