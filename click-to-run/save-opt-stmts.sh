#! /bin/bash

data_dir="${WETUNE_DATA_DIR:-wtune_data}"
verbose='0'
optimizer="WeTune"
rewrite_dir="rewrite"

while [[ $# -gt 0 ]]; do
  case "$1" in
  "-v" | "-verbose")
    verbose="${2}"
    shift 2
    ;;
  "-opt" | "-optimizer")
    optimizer="${2}"
    shift 2
    ;;
  *)
    positional_args+=("${1}")
    shift
    ;;
  esac
done

gradle :superopt:run --args="UpdateOptStmts -v=${verbose} -optimizer=${optimizer} "
