#! /bin/bash

verbose='0'
rules='rules.txt'

while [[ $# -gt 0 ]]; do
  case "$1" in
  "-rules")
    rules="${2}"
    shift 2
    ;;
  "-verbose")
    verbose="${2}"
    shift 2
    ;;
  *)
    postional_args+=("${1}")
    shift
    ;;
  esac
done

set -- "${postional_args[@]}"

gradle :superopt:run --args="runner.OptimizeQuery -v=${verbose} -R=${rules}"

cd 'wtune_data/opt' || exit

dir=$(ls -t -1 | ag 'run.+' | head -1)
ln -sfr "${dir}" 'result'
