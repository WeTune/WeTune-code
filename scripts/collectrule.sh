#!/bin/bash

set -eu -o pipefail

keep="10000000"

while [[ $# -gt 0 ]]; do
  case $1 in
  -keep)
    keep="$2"
    shift 2
    ;;
  *)
    shift
    ;;
  esac
done

t=$(date '+%m_%d_%H_%M')

cd 'wtune_data/enumerations'
name="rules.${t}.txt"
files=$(ls -t -1 | ag 'run.+')

while IFS= read -r line; do
  echo "${keep}"
  if [ "${keep}" = 0 ]; then
    break
  fi
  if [ -f "${line}/success.txt" ]; then
    cat "${line}/success.txt" >>"${name}"
    keep=$((keep - 1))
  fi
done <<<"${files}"

ln -sfr "${name}" "../rules.partial.txt"
