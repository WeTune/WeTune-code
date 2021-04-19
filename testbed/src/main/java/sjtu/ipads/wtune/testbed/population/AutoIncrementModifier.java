package sjtu.ipads.wtune.testbed.population;

class AutoIncrementModifier implements Modifier {
  AutoIncrementModifier() {}

  @Override
  public void modify(int seed, Actuator actuator) {
    // `seed` here should be a row number
    // for auto-increment column, the value at the i-th row is just i+1
    actuator.appendInt(seed + 1); // row number is 0-based, plus 1
  }
}
