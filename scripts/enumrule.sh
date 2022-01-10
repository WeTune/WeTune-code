#! /bin/bash

postional_args=()
while [[ $# -gt 0 ]]; do
  case "$1" in
  --rerun)
    rerun=1
    shift
    ;;
  *)
    postional_args+=("${1}")
    shift
    ;;
  esac
done

set -- "${postional_args[@]}"

if [ -d "wtune_data/enumerations" ]; then
  cwd="${PWD}"
  cd 'wtune_data/enumerations' || true
  files=$(ls -t -1 | ag 'run.+')

  if [[ -z "$rerun" ]]; then
    while IFS= read -r line; do
      if [ -f "${line}/checkpoint" ]; then
        checkpoint="${line}/checkpoint"
        break
      fi
    done <<<"${files}"
  fi
  cd "${cwd}" || exit
fi

cd ..

if [ -z "${checkpoint}" ]; then
  gradle :superopt:run --args="runner.EnumRule -parallelism=32 -timeout=600000 ${1} ${2}"
else
  gradle :superopt:run --args="runner.EnumRule -parallelism=32 -timeout=600000 -checkpoint=${checkpoint} ${1} ${2}"
fi
