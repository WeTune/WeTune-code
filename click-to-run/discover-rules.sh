#! /bin/bash

parallelism=4
timeout=900000
verbose=0
num_partitions=32
num_cpus=$(grep -c ^processor /proc/cpuinfo)

# read arguments
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
  "-partition")
    partition="${2}"
    shift 2
    ;;
  *)
    positional_args+=("${1}")
    shift
    ;;
  esac
done

# parse partition
if [ -z "${partition}" ]; then
  ((num_partitions = num_cpus / parallelism))
  ((from_partition = 0))
  ((to_partition = num_partitions - 1))
else
  IFS='/' read -ra TMP <<<"${partition}"
  num_partitions="${TMP[0]:-${num_partitions}}"
  IFS='-' read -ra TMP <<<"${TMP[1]}"
  from_partition="${TMP[0]:-0}"
  to_partition="${TMP[1]:-${from_partition}}"

  if [ "${from_partition}" -gt "${to_partition}" ]; then
    echo "Wrong partition spec: ${num_partitions}/${from_partition}-${to_partition}"
    exit 1
  fi
fi

gradle :superopt:compileJava >/dev/null 2>&1 || echo \
  'Error in compilation. Run `gradle compileJava` to check error.' && exit 1

echo "Begin rule discovery. "
echo "#process=${num_partitions}  #threads_per_process=${parallelism} timeout_per_pair=${timeout}ms"

for ((i = from_partition; i <= to_partition; i++)); do
  nohup gradle :superopt:run \
    --args="EnumRule -v=${verbose} -parallelism=${parallelism} -timeout=${timeout} -partition=${num_partitions}/${i}" \
    >"enum.log.${i}" 2>&1 &
done