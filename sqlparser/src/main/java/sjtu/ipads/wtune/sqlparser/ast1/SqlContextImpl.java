package sjtu.ipads.wtune.sqlparser.ast1;

import sjtu.ipads.wtune.common.tree.LabeledTreeContextBase;
import sjtu.ipads.wtune.sqlparser.schema.Schema;

public class SqlContextImpl extends LabeledTreeContextBase<SqlKind> implements SqlContext {
  private Schema schema;
  private String dbType;

  protected SqlContextImpl(int expectedNumNodes, Schema schema) {
    super(expectedNumNodes);
    this.schema = schema;
  }

  @Override
  public Schema schema() {
    return schema;
  }

  @Override
  public String dbType() {
    return dbType;
  }

  @Override
  public void setSchema(Schema schema) {
    this.schema = schema;
  }

  @Override
  public void setDbType(String dbType) {
    this.dbType = dbType;
  }
}
