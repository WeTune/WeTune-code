# WeTune

This package includes the source code and the testing scripts in the paper
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

## Part I: Getting Start Guide

### Environment Setup

#### Requirements

* Java 17
* Gradle 7.3.3
* z3 4.8.9  *(SMT solver)*
* antlr 4.8  *(Generate tokenizer and parser for SQL AST)*

Please use the scripts `{TODO}` to install Java and Gradle. z3 and antlr library have been put in `lib/` off-the-shelf.

#### Compilation

```shell
gradle compileJava
```

### WeTune Workflow

This section gives the instruction of the whole workflow of WeTune, including

1. rule discovery
2. rewriting queries using rules
3. pick useful rules by evaluating the rewritings.

The whole procedure typically takes several days (mainly for rule discovery). If you are particularly curious about how
WeTune works, please refer to Section [Run Example](#run-examples), which gives instructions of running individual
examples in each step and inspecting the internal of WeTune. Also, please refer to Section [SPES](#spes) for the
comparison with SPES verifier and the evaluation of integrating SPES verifier.

#### Discover Rules

```shell
click-to-run/discover-rules.sh  # launches background processes
# Inspect progress:
click-to-run/show-progress.sh
# After the all processes finished:
click-to-run/collect-rules.sh 
click-to-run/reduce-rules.sh
```

The first commands launches many processes running in the background. Note they will consume all CPUs and takes a long
time (~`{TODO}` hours) to finish. Use `click-to-run/show-progress.sh` to inspect the progress of each process. The
discovered rules so far can be found in `wtune_data/enumeration/run_*/success.txt` (`*` is a timestamp).

*Rationale: z3 incurs high inter-thread lock contention. The script uses multi-process instead of multi-thread to
mitigate this problem.*

The second commands aggregates `wtune_data/enumeration/run_*/succcess.txt`, and outputs
to `wtune_data/rules/rules.local.txt`.

The third commands reduces rules (see Section `{TODO}` in paper) and outputs the reduced rules
to `wtune_data/rules/rules.txt`. This command typically finishes in 30-50 minutes, depending on the number of discovered
rules.

The overall running time can be limited by a smaller `-timeout`. Please refer to [Part II](#part-ii) for details.
However, too short timeout may impact the usefulness of discovered rules.

#### Rewrite Queries Using Discovered Rules

```shell
click-to-run/rewrite-queries.sh
```

This script uses `wtune_data/rules/rules.txt` to rewrite queries stored in `wtune_data/wtune.db`. It usually finishes in
30 minutes. The output can be found in `wtune_data/rewrite/result/1_query.tsv`.

#### Evaluate the Rewritings

```shell
click-to-run/populate-data.sh
click-to-run/estimate-cost.sh
click-to-run/profile-cost.sh
```

`{TODO}`

### Run Examples

## Part II: Step-by-Step Instructions