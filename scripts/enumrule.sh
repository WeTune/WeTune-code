#! /bin/bash

positional_args=()
parallelism=32
timeout=600000
verbose=0

while [[ $# -gt 0 ]]; do
  case "$1" in
  "-parallelism")
    parallelism="${2}"
    shift 2
    ;;
  "-timeout")
    timeout="${2}"
    shift 2
    ;;
  "-verbose")
    verbose="${2}"
    shift 2
    ;;
  "-rerun")
    rerun="true"
    shift
    ;;
  *)
    positional_args+=("${1}")
    shift
    ;;
  esac
done

set -- "${positional_args[@]}"

if [[ -z "$rerun" ]]; then
  if [ -d "wtune_data/enumerations" ]; then
    cwd="${PWD}"
    cd 'wtune_data/enumerations' || true
    files=$(ls -t -1 | ag 'run.+')

    while IFS= read -r line; do
      if [ -f "${line}/checkpoint.txt" ]; then
        checkpoint="enumerations/${line}/checkpoint.txt"
        break
      fi
    done <<<"${files}"

    cd "${cwd}" || exit
  fi
fi

partitions=${1:-'1'}
local_idx=${2:-'0'}

if [ -z "${checkpoint}" ]; then
  gradle :superopt:run --args="runner.EnumRule -v=${verbose} -parallelism=${parallelism} -timeout=${timeout} -partition=${partitions}/${local_idx}"
else
  gradle :superopt:run --args="runner.EnumRule -v=${verbose} -parallelism=${parallelism} -timeout=${timeout} -checkpoint=${checkpoint} -partition=${partitions}/${local_idx}"
fi
