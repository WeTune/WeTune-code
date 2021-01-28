package sjtu.ipads.wtune.stmt.support.internal;

import sjtu.ipads.wtune.stmt.support.Issue;

public record IssueImpl(String app, int stmtId) implements Issue { }
