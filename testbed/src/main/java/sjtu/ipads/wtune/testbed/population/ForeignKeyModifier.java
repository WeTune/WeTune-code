package sjtu.ipads.wtune.testbed.population;

class ForeignKeyModifier implements Modifier {
  private final Generator delegation;
  private final int delegationRowCount;

  ForeignKeyModifier(Generator delegation, int delegationRowCount) {
    this.delegation = delegation;
    this.delegationRowCount = delegationRowCount;
  }

  @Override
  public void modify(int seed, Actuator actuator) {
    delegation.generate(seed % delegationRowCount, actuator);
  }
}
