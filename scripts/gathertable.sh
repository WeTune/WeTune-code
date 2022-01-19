#! /bin/bash

queries='rewrite/result/1_query.tsv'

while [[ $# -gt 0 ]]; do
  case "$1" in
  "-queries")
    queries="${2}"
    shift 2
    ;;
  *)
    postional_args+=("${1}")
    shift
    ;;
  esac
done

set -- "${postional_args[@]}"

gradle :superopt:run --args="runner.GatherAccessedTables -i=${queries}"
