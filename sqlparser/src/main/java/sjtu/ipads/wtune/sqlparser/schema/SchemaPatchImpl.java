package sjtu.ipads.wtune.sqlparser.schema;

import java.util.List;
import sjtu.ipads.wtune.sqlparser.schema.SchemaPatch;

record SchemaPatchImpl(Type type, String schema, String table, List<String> columns, String reference) implements SchemaPatch {}
