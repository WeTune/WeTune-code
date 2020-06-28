package sjtu.ipads.wtune.stmt.dao.internal;

import sjtu.ipads.wtune.sqlparser.SQLParser;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.dao.SchemaDao;
import sjtu.ipads.wtune.stmt.schema.Schema;

import java.io.IOException;
import java.nio.file.Files;

public class FileSchemaDao implements SchemaDao {
  @Override
  public Schema findOne(String appName, String dbType) {
    try {
      final String content =
          Files.readString(
              Setup.current().dataDir().resolve("schemas").resolve(appName + ".schema.sql"));

      final SQLParser parser = SQLParser.ofDb(dbType);
      final Schema schema = new Schema();

      SQLParser.splitSql(content).stream().map(parser::parse).forEach(schema::addDefinition);

      return schema;
    } catch (IOException e) {
      throw new StmtException(e);
    }
  }
}
