#!/bin/bash
cd wtune_data

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
name="rule.${t}.txt"

files=$(ls -t -1 | ag 'rule.+')

while IFS= read -r line; do
  if [ "${keep}" = 0 ]; then
      break
  fi
  if [ -f "${line}/success" ]; then
    cat "${line}/success" >>"${name}"
    keep=$((keep - 1))
  fi
done <<<"${files}"

ln -sfr "${name}" "rules.partial.txt"
