#! /bin/bash

data_dir="${WETUNE_DATA_DIR:-wtune_data}"
verbose='0'
rewrite_dir="rewrite"

while [[ $# -gt 0 ]]; do
  case "$1" in
  "-v" | "-verbose")
    verbose="${2}"
    shift 2
    ;;
  *)
    positional_args+=("${1}")
    shift
    ;;
  esac
done

gradle :superopt:run --args="PickMinCost -v=${verbose} "

cwd=$(pwd)
cd "${data_dir}/${rewrite_dir}" || exit

echo "$(cut -f1,2 'result/2_query.tsv' | uniq | wc -l) queries rewritten."

cd "${cwd}" || exit