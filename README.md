# WeTune

This codebase includes the source code and the testing scripts in the paper
*Automatic Discovery and Verification of Query Rewrite Rules*

```shell
.
|-- click-to-run    # Click-to-run scripts for Part I.
|-- lib             # Required external library.
|-- common          # Common utilities.
|-- sql             # Data structures of SQL AST and query plan.
|-- stmt            # Manager of queries from open-source applications.
|-- superopt        # Core algorithm of WeTune.
    |-- fragment    # Plan template enumeration.
    |-- constraint  # Constraint enumeration.
    |-- uexpr       # U-expression.
    |-- logic       # SMT-based verifier.
    |-- optimizer   # Rewriter.
|-- testbed         # Evaluation framework.
|-- spes/           # SPES-based verifier.
|-- wtune_data/     # Data input/output directory.
    |-- schemas/    # Schemas of applications.
    |-- wtune.db    # Sqlite DB storing the persistent statistics
```

**Still under development and not ready for release.**

## Environment Setup

### Requirements

* Java 17
* Gradle 7.3.3
* z3 4.8.9  *(SMT solver)*
* antlr 4.8  *(Generate tokenizer and parser for SQL AST)*
* Microsoft SQL Server `{TODO:version}` *(Evaluate the usefulness of rules)*

Please run

```shell
TODO
```

to install Java, Gradle and SQL Server. z3 and antlr library have been put in `lib/` off-the-shelf.

### Compilation

```shell
gradle compileJava
```

## WeTune Workflow

This section gives the instruction of the whole workflow of WeTune, including

1. rule discovery
2. rewriting queries using rules
3. pick useful rules by evaluating the rewritings.

The whole procedure typically takes several days (mainly for rule discovery). If you are particularly interested in how
WeTune works, please refer to Section [Run Example](#run-examples), which gives instructions of running individual
examples in each step and inspecting the internal of WeTune.

### Discover Rules

```shell
click-to-run/discover-rules.sh  # launches background processes
# Check progress:
click-to-run/show-progress.sh
# After the all processes finished:
click-to-run/collect-rules.sh 
click-to-run/reduce-rules.sh
```

The first commands launches many processes running in the background. Note they will consume all CPUs and takes a long
time (~3600 CPU hours) to finish. Use `click-to-run/show-progress.sh` to inspect the progress of each process. The
discovered rules so far can be found in `wtune_data/enumeration/run_*/success.txt` (`*` is a timestamp).

The second commands aggregates `wtune_data/enumeration/run_*/succcess.txt`, and outputs
to `wtune_data/rules/rules.local.txt`.

The third commands reduces rules (see Section 7 in paper) and outputs the reduced rules to `wtune_data/rules/rules.txt`.
This command typically finishes in 30-50 minutes, depending on the number of discovered rules.


> **Why multi-process instead of multi-thread?**
>
> z3 incurs high inter-thread lock contention. The script uses multi-process instead of multi-thread to mitigate this problem.

> The overall running time can be reduced by limiting the time budget of each plan template pair to searching for most-relaxed
> constraint set (Section 4.3 in the paper). Please refer to [Part II](#part-ii) for details. However, too short timeout may impact the usefulness of discovered rules.

### Rewrite Queries Using Discovered Rules

```shell
click-to-run/rewrite-queries.sh
```

This script uses `wtune_data/rules/rules.txt` to rewrite queries stored in `wtune_data/wtune.db`. It usually finishes in
30 minutes. The output can be found in `wtune_data/rewrite/result/1_query.tsv`.

### Evaluate the Rewritings

```shell
click-to-run/populate-data.sh
click-to-run/estimate-cost.sh
click-to-run/profile-cost.sh
```

`{TODO}`

## Run Examples

This section provides the instruction of run examples:

* template enumeration
* rule verification
* constraint enumeration

### Template Enumeration Example

```shell
click-to-run/run-template-example.sh
```

All templates of max size 4 (after pruning) will be printed, 3113 in total.

### Rule Verification

```shell
click-to-run/run-verify-example.sh [rule_index]
```

`<rule_index>` can be 1-35, corresponds to Table 7 in the paper.

For each rule, the following items will be printed:

* The rule itself.
* A query q0, on which the rule can be applied.
* A query q1, the rewrite result after applying the rule to q0.

For rule 1-31, which can be proved by WeTune built-in verifier, these additional items will be printed:

* A pair of U-Expression, translated from the rule.
* One or more Z3 snippets, the formula submitted to Z3.

> **Why there can be more than one snippet?**
>
> To relief the burden of Z3, When we are going to prove `(p0 /\ p1 ... /\ pn) /\ (q \/ r)` is UNSAT, we separately prove that `(p0 /\ p1 ... /\ pn) /\ q`
and `(p0 /\ p1 /\ ... /\ pn)` are both UNSAT. This is particularly the case when applying theorem 5.2.

### Constraint Enumeration

```shell
click-to-run/run-enum-example.sh [-dump] <rule_index>
```

`<rule_index>` can be 1-35, corresponds to Table 7 in the paper.

`-dump` specifies whether to dump all searched constraint sets to output.

WeTune will enumerate the constraints between the plan template of given rule, and search for the most-relaxed
constraint sets. Each of the examined constraint set and its verification result will be printed. The found rules and
metrics will be appended after enumeration completes.

P.S. If `-dump` is specified, for some pairs, the output floods for a few minutes, you may want to dump it to a file.
