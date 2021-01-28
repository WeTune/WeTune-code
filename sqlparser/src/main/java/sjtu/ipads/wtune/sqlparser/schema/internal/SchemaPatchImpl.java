package sjtu.ipads.wtune.sqlparser.schema.internal;

import sjtu.ipads.wtune.sqlparser.schema.SchemaPatch;

import java.util.List;

public record SchemaPatchImpl(Type type, String schema, String table, List<String> columns) implements SchemaPatch {}
