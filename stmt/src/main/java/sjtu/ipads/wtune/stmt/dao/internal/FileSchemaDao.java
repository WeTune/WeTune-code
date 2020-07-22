package sjtu.ipads.wtune.stmt.dao.internal;

import sjtu.ipads.wtune.sqlparser.SQLParser;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.dao.SchemaDao;
import sjtu.ipads.wtune.stmt.dao.SchemaPatchDao;
import sjtu.ipads.wtune.stmt.schema.Schema;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileSchemaDao implements SchemaDao {
  @Override
  public Schema findOne(String appName, String tag, String dbType) {
    try {
      final Path filePath =
          Setup.current().dataDir().resolve("schemas").resolve(appName + "." + tag + ".schema.sql");
      final String content = Files.readString(filePath);

      final SQLParser parser = SQLParser.ofDb(dbType);
      if (parser == null) return null;

      final Schema schema = new Schema();
      schema.setSourcePath(filePath);

      SQLParser.splitSql(content).stream().map(parser::parse).forEach(schema::addDefinition);
      schema.buildRefs();

      SchemaPatchDao.instance().findByApp(appName).forEach(schema::addPatch);

      return schema;
    } catch (IOException e) {
      throw new StmtException(e);
    }
  }
}
