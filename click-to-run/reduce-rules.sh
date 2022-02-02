#!/bin/bash

data_dir="${WETUNE_DATA_DIR:-wtune_data}"
rules_dir='rules'
out="${data_dir}/${rules_dir}/rules.txt"
in="${data_dir}/${rules_dir}/rules.raw.txt"
if ! [ -f "${in}" ]; then
  in="${data_dir}/${rules_dir}/rules.local.txt"
fi

# read arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
  "-f" | "-file" | "-i" | "-in" | "-input" | "-R" | "rules")
    in="${2}"
    shift 2
    ;;
  "-o" | "-out" | "-output")
    out="${2}"
    shift 2
    ;;
  *)
    positional_args+=("${1}")
    shift
    ;;
  esac
done

echo "Begin rule reducing."
gradle :superopt:run --args="ReduceRules -R=${in} -o=${out} -a=none"
echo "$(wc -l "${out}") non-reducible rules discovered."
