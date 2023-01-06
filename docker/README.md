The code and scripts provided in this repository are used for the purpose of reproducibility.
The requirements for the machine are listed as follows:
Required OS: Linux.
Required apps: Docker, bash.
Required machine: at least 500 GiB of disk space for the docker container, most of which is used for storing tables when evaluating large workloads in SQL Server.

Here are the steps to execute the repro.sh:
1. Download the repository of WeTune to your machine.
2. Enter the directory wetune/docker.
3. Update HOST_DUMP_PATH, HOST_MSSQL_PATH in the repro.sh script, so they point to the appropriate paths on the host machine. We suggest 350GiB(at least 250GiB) for HOST_DUMP_PATH and 650GiB (at least 600GiB)for HOST_MSSQL_PATH
4. Execute repro.sh to start the experiments.

When repro.sh finishes, the result will be copied to the host machine under the directory wetune/docker/result_from_docker. The directory structure is as follows:

```
wetune/docker/result_from_docker
├── calcite
|	└── result 
|
├── profile
│   └── result                                # the profiling results of four different workloads
│       ├── base
│       ├── zipf
│       ├── large
│       ├── large_zipf
|
├── profile_calcite
│   └── result                                # the profiling result of four calcite
│       ├── base
|
├── rewrite
│   ├── result
│       ├── 1_query.tsv                       # all rewritten queries
│       ├── 1_rules.tsv                       # rules used to rewrite queries in 1_query.tsv
│       ├── 2_query.tsv                       # the rewritten queries picked with the minimal cost by asking the database's cost |		 | 									   # model
│       └── 2_trace.tsv                       # used rules of each rewritten query in 2_query.tsv
└── viewall
│   ├── result
│        ├── optimizationInfo.tsv             # optimization info of queries
│        └── usefulRules.tsv                  # all the used rules in the process of optimization
│
└── viewall_calcite
│	├── result
│		├── statistic.tsv					  # optimization info of Calcite test suite
│		├── wetuneSurpassesCalcite.tsv		  # queries WeTune achieves a better performance of rewriting than Calcite itself 
└── viewall_statistics
|   ├── result
|       ├── base.tsv
|       └── statistics						  # the statistics collected for the four workloads for the third checking point 
|
└── enumeration
│        ├── run_*                            # discovered rules so far ( * here is a timestamp)
│
└── rules.txt                                 # the resulting rules that have been reduced
```

Note: The two containers, `wetune` and `mssql`, communicate through port 1433 of the host machine. So if you have any process using port 1433, you should stop it to free the port.
