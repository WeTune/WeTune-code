#! /bin/bash

verbose=0
target="opt_used"
optimizer="WeTune"
tag="base"

# read arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
  "-v" | "-verbose")
    verbose="${2}"
    shift 2
    ;;
  "-T" | "-target")
    target="${2}"
    shift 2
    ;;
  "-optimizer")
    optimizer="${2}"
    shift 2
    ;;
  "-t" | "-tag")
    tag="${2}"
    shift 2
    ;;
  *)
    positional_args+=("${1}")
    shift
    ;;
  esac
done

echo "Begin populating data of ${tag} workload"
gradle :testbed:run \
    --args="GenerateTableData -v=${verbose} -target=${target} -optimizer=${optimizer} -tag=${tag}"
echo "Finish populating data of ${tag} workload"