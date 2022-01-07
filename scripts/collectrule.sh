#!/bin/bash

set -eu -o pipefail

keep="10000000"

while [[ $# -gt 0 ]]; do
    case $1 in
        --keep)
            keep="$2"
            shift 2
            ;;
        *)
            shift
            ;;
    esac
done

t=$(date '+%m_%d_%H_%M')

cd wtune_data
dirname="enumerations"
mkdir "${dirname}" 2>/dev/null || true
name="rules.${t}.txt"

files=$(ls -t -1 | ag 'rule.+')

while IFS= read -r line; do
  if [ "${keep}" = 0 ]; then
      break
  fi
  if [ -f "${line}/success" ]; then
    cat "${line}/success" >>"${dirname}/${name}"
    keep=$((keep - 1))
  fi
done <<<"${files}"

ln -sfr "${dirname}/${name}" "rules.partial.txt"
