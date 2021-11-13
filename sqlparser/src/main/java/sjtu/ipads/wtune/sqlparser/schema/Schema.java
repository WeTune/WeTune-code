package sjtu.ipads.wtune.sqlparser.schema;

import java.util.Collection;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.AstSupport.parseSql;
import static sjtu.ipads.wtune.sqlparser.AstSupport.splitSql;

public interface Schema {
  Collection<? extends Table> tables();

  String dbType();

  Table table(String name);

  void patch(Iterable<SchemaPatch> patches);

  StringBuilder toDdl(String dbType, StringBuilder buffer);

  static Schema parse(String dbType, String str) {
    return SchemaImpl.build(dbType, listMap(splitSql(str), s -> parseSql(dbType, s)));
  }
}
