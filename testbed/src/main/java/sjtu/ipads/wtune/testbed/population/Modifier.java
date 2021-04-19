package sjtu.ipads.wtune.testbed.population;

public interface Modifier extends Generator {
  void modify(int seed, Actuator actuator);

  @Override
  default void generate(int seed, Actuator actuator) {
    modify(seed, actuator);
  }
}
