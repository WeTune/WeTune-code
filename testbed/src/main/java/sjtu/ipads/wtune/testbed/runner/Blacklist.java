package sjtu.ipads.wtune.testbed.runner;

import sjtu.ipads.wtune.stmt.Statement;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class Blacklist {
  private final Map<String, Set<String>> blacklists;

  Blacklist() {
    this.blacklists = new HashMap<>();
  }

  boolean isBlocked(String app, Statement stmt) {
    return blacklists.get(app).contains(stmt.toString());
  }
}
