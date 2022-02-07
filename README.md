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
* Microsoft SQL Server 2019 *(Evaluate the usefulness of rules)*

z3 and antlr library have been put in `lib/` off-the-shelf. Please use to deploy the other environment.

```shell
click-to-run/environment-setup.sh
```

On the AWS machine, all requirements have been deployed.

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
# Launch background processes to run rule discovery
click-to-run/discover-rules.sh  

# After the all processes finished:
click-to-run/collect-rules.sh && click-to-run/reduce-rules.sh

# Check progress:
click-to-run/show-progress.sh

# Use this to terminate all process
click-to-run/stop-discovery.sh
```

The first commands launches many processes running in the background. Note they will consume all CPUs and takes a long
time (~3600 CPU hours) to finish. Use `click-to-run/show-progress.sh` to inspect the progress of each process. The
discovered rules so far can be found in `wtune_data/enumeration/run_*/success.txt` (`*` is a timestamp).

The second commands aggregates `wtune_data/enumeration/run_*/succcess.txt` and reduce rules (Section 7 in paper)
and outputs the reduced rules to `wtune_data/rules/rules.txt`.

The third are used to check the progress. The fourth can terminate all background tasks launched by the first command.


> **Why multi-process instead of multi-thread?**
>
> z3 incurs high inter-thread lock contention. The script uses multi-process instead of multi-thread to mitigate this problem.

### Rewrite Queries Using Discovered Rules

```shell
click-to-run/rewrite-queries.sh
```

This script uses `wtune_data/rules/rules.txt` to rewrite queries stored in `wtune_data/wtune.db`. It usually finishes in
30 minutes. The output can be found in `wtune_data/rewrite/result/1_query.tsv`.

### Evaluate the Rewritings

```shell
click-to-run/make-db.sh
click-to-run/generate-data.sh
```

These scripts generate and insert data into Sql Server database for use of evaluation.

Use `click-to-run/make-db.sh` to create databases and corresponding schemas in Sql Server.

Use `click-to-run/generate-data.sh` to generate data and import data to Sql Server.
Dumped data files can be found in directory `wtune_data/dump/`.

```shell
click-to-run/estimate-cost.sh
click-to-run/profile-cost.sh
```

These scripts pick the optimized queries and profile them using Sql Server database.

`click-to-run/estimate-cost.sh` takes previously generated file `wtune_data/rewrite/result/1_query.tsv` as input and
pick one rewritten query with the minimal cost by asking the database's cost model.
The result will be stored in `wtune_data/rewrite/result/2_query.tsv` and used rules will be stored in 
`wtune_data/rewrite/result/2_trace.tsv`.

And `click-to-run/profile-cost.sh` profiles the optimized queries. The output file is in `wtune_data/profile/` by
default.

**Please refer to [Evaluation Configuration](#Evaluation Configuration) for more details about parameters of the scripts in this section.**

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

## Evaluation Configuration

### Workload types

In the paper, we evaluate queries on 4 different workload types:

| Workload type | # of rows | Data distribution |
|---------------|-----------|-------------------|
| base          | 10 k      | uniform           |
| zipf          | 10 k      | zipfian           |
| large         | 1 M       | uniform            |
| large_zipf    | 1 M       | zipfian           |

### Prover types

Currently, we list 2 kinds of verifier: `WeTune` and `Spes`. 

### Parameter selection

By default, the scripts above evaluate queries optimized by `WeTune` on workload of `base` type.
However, for example, if you would like to evaluate on different type of workload 
or evaluate queries optimized by a different verifier, you can set additional parameters to
the scripts in section [Evaluate the Rewritings](#Evaluate the Rewritings):

```shell
click-to-run/make-db.sh
click-to-run/generate-data.sh [-tag] <workload_type> [-optimizer] <verifier_type>
```
```shell
click-to-run/estimate-cost.sh 
click-to-run/profile-cost.sh [-tag] <workload_type> [-optimizer] <verifier_type>
```
For example, to evaluation queries optimized by `Spes` on workload type of `zipf`, run: 
```shell
click-to-run/make-db.sh
click-to-run/generate-data.sh -tag zipf -optimizer Spes
```
```shell
click-to-run/estimate-cost.sh 
click-to-run/profile-cost.sh -tag zipf -optimizer Spes
```