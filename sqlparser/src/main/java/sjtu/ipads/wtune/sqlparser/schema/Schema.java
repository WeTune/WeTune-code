package sjtu.ipads.wtune.sqlparser.schema;

import sjtu.ipads.wtune.common.utils.ListSupport;
import sjtu.ipads.wtune.sqlparser.ast1.SqlNode;

import java.util.Collection;
import java.util.function.Function;

import static sjtu.ipads.wtune.sqlparser.SqlSupport.parseSql;
import static sjtu.ipads.wtune.sqlparser.SqlSupport.splitSql;

public interface Schema {
  Collection<? extends Table> tables();

  String dbType();

  Table table(String name);

  void patch(Iterable<SchemaPatch> patches);

  StringBuilder toDdl(String dbType, StringBuilder buffer);

  static Schema parse(String dbType, String str) {
    return SchemaImpl.build(dbType, ListSupport.map((Iterable<String>) splitSql(str), (Function<? super String, ? extends SqlNode>) s -> parseSql(dbType, s)));
  }
}
