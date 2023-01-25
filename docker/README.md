The scripts provided in `{projRootDir}/docker` are used for reproducibility.
The requirements for the machine are listed as follows:
* OS: Linux
* Apps: Docker, bash
* Disk space: at least 750 GiB disk space for the docker container *(most of which is used for storing tables when evaluating large workloads in SQL Server)*

Here are the steps to follow:
1. Download the repository of WeTune to your machine.
2. Enter the directory `{projRootDir}/docker`.
3. Update `HOST_DUMP_PATH`, `HOST_MSSQL_PATH` in the `repro.sh` script, so they point to the appropriate paths on the host machine. 
We suggest 350GiB (at least 250GiB) for `HOST_DUMP_PATH` and 650GiB (at least 600GiB) for `HOST_MSSQL_PATH`
4. Execute `repro.sh` to start the experiments.
5. Execute `repro-issue.sh` to get the rewriting result of 50 GitHub issues mentioned in the paper. 

When the scripts finish, the result will be copied to the host machine under the directory 
`{projRootDir}/docker/result_from_docker` and `{projRootDir}/docker/issue_result_from_docker`. 
The directory structures are as follows:

```
./docker/result_from_docker/
├── calcite
│   └── result
│       ├── 1_query.tsv
│       ├── 1_rules.tsv
│       ├── 2_query.tsv
│       └── 2_trace.tsv
├── profile
│   └── result                            # the profiling results of four different workloads
│       ├── base
│       ├── large
│       ├── large_zipf
│       └── zipf
├── profile_calcite
│   └── result                            # the profiling result of four calcite
│       ├── base
│       ├── large
│       ├── large_zipf
│       └── zipf
├── rewrite
│   └── result
│       ├── 1_query.tsv                   # all rewritten queries
│       ├── 1_rules.tsv                   # rules used to rewrite queries in 1_query.tsv
│       ├── 2_query.tsv                   # the rewritten queries picked with the minimal cost by asking the database's cost
│       └── 2_trace.tsv                   # used rules of each rewritten query in 2_query.tsv
├── viewall
│   └── result
│       ├── optimizationInfo.tsv          # optimization info of queries
│       └── usefulRules.tsv               # all the used rules in the process of optimization
├── viewall_calcite
│   └── result
│       ├── statistic.tsv                 # optimization info of Calcite test suite
│       └── wetuneSurpassesCalcite.tsv    # queries WeTune achieves a better performance of rewriting than Calcite itself 
├── viewall_statistics
│    └── result
│       ├── base.tsv
│       ├── large.tsv
│       ├── large_zipf.tsv
│       ├── statistics                    # the statistics collected for the four workloads for the third checking point
│       └── zipf.tsv
├── enumeration
│        └── run_*                        # discovered rules so far ( * here is a timestamp)
│
└── rules.txt                             # the resulting rules that have been reduced
```

```
./docker/issue_result_from_docker/
└── issues
    ├── calcite_rewrites                                    # rewriting result of Apache Calcite
    ├── issues                                              # raw information of issues
    └── result                                              # the profiling results of four different workloads
        ├── {appName}#{issueId}                             # rewriting result of one certain issue identified by {appName}#{issueId}
        └── ...                                # raw information of issues
```

Notes: The two containers, `wetune` and `mssql`, communicate through port 1433 of the host machine. 
So if you have any process using port 1433, you should stop it to free the port.
