package sjtu.ipads.wtune.sql.ast1;

public interface AdditionalInfo<T extends AdditionalInfo<T>> {
  interface Key<T extends AdditionalInfo<T>> {
    T init(SqlContext sql);
  }

  void renumberNode(int oldId, int newId);

  void deleteNode(int nodeId);
}
