package sjtu.ipads.wtune.superopt.operators.impl;

import sjtu.ipads.wtune.superopt.Operator;
import sjtu.ipads.wtune.superopt.Relation;

public abstract class BaseOperator implements Operator {
  private Operator next;
  private final Operator[] prev;

  private Relation out;
  private Relation[] in;

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
    this.in = null;
    return true;
  }

  @Override
  public Relation out() {
    return out; // TODO
  }

  @Override
  public Relation[] in() {
    if (in == null) {
      in = new Relation[prev.length];
      for (int i = 0; i < prev.length; i++) in[i] = prev[i] == null ? null : prev[i].out();
    }

    return in;
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
