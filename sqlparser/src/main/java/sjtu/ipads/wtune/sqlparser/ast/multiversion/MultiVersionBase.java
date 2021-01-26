package sjtu.ipads.wtune.sqlparser.ast.multiversion;

public abstract class MultiVersionBase<C, B> implements MultiVersion {
  protected C current;
  protected B prev;

  protected MultiVersionBase() {}

  protected MultiVersionBase(C current, B prev) {
    this.current = current;
    this.prev = prev;
  }

  @Override
  public void derive() {
    if (current != null) prev = makePrev(current, prev);
    current = makeCurrent();
  }

  @Override
  public Snapshot snapshot() {
    return Snapshot.singleton(makePrev(current, prev));
  }

  @Override
  public void setSnapshot(Snapshot snapshot) {
    final MultiVersionBase<C, B> b = snapshot.get(this.getClass());
    current = b.current;
    prev = b.prev;
  }

  protected abstract C makeCurrent();

  protected abstract B makePrev(C current, B prev);
}
