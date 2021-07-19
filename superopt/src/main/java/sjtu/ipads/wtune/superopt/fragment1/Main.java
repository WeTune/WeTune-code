package sjtu.ipads.wtune.superopt.fragment1;

import sjtu.ipads.wtune.superopt.substitution.Substitution;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {
  public static void main(String[] args) throws IOException {
    final List<String> lines = Files.readAllLines(Paths.get("wtune_data", "filtered_bank"));
    for (String line : lines) {
      if (line.charAt(0) == '=') continue;
      final Substitution sub = Substitution.parse(line);
      System.out.println(sub);
    }
  }
}
