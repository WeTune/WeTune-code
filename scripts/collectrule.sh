#!/bin/bash
cd wtune_data

t=$(date '+%m_%d_%H_%M')
name="${t}_rules"

files=$(ls -t -1 | ag 'rule.+')

while IFS= read -r line; do
  if [ -f "${line}/success" ]; then
    cat "${line}/success" >>"${name}"
  fi
done <<<"${files}"
