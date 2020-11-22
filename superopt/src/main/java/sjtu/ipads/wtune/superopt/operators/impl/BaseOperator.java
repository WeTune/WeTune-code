package sjtu.ipads.wtune.superopt.operators.impl;

import sjtu.ipads.wtune.superopt.operators.Operator;
import sjtu.ipads.wtune.superopt.relational.RelationSchema;

public abstract class BaseOperator implements Operator {
  private Operator next;
  private final Operator[] prev;

  private RelationSchema outSchema;

  private int id;

  protected BaseOperator() {
    this(1);
  }

  protected BaseOperator(int numChildren) {
    this.prev = new Operator[numChildren];
  }

  @Override
  public Operator next() {
    return next;
  }

  @Override
  public Operator[] prev() {
    return prev;
  }

  @Override
  public boolean setNext(Operator next) {
    this.next = next;
    return true;
  }

  @Override
  public boolean setPrev(int idx, Operator prev) {
    if (prev != null) {
      this.prev[idx] = prev;
      prev.setNext(this);
    } else {
      prev = this.prev[idx];
      this.prev[idx] = null;
      if (prev != null) prev.setNext(null);
    }
    return true;
  }

  @Override
  public void setId(int id) {
    this.id = id;
  }

  @Override
  public int id() {
    return id;
  }

  @Override
  public RelationSchema outSchema() {
    return outSchema;
  }

  @Override
  public void setOutSchema(RelationSchema outSchema) {
    this.outSchema = outSchema;
  }

  @Override
  public Operator copy0() {
    final Operator newInstance = newInstance();
    copyTo(newInstance);
    return newInstance;
  }

  protected abstract Operator newInstance();

  protected void copyTo(Operator other) {}
}
