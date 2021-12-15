#! /bin/bash
cd wtune_data

files=$(ls -t -1 | ag 'rule.+')
checkpoint=

while IFS= read -r line; do
  if [ -f "${line}/checkpoint" ]; then
    checkpoint="${line}/checkpoint"
    break
  fi
done <<<"${files}"

cd ..

if [ -z "${checkpoint}" ]; then
  gradle :superopt:run --args="runner.EnumRule -parallelism=32 ${1} ${2}"
else
  gradle :superopt:run --args="runner.EnumRule -parallelism=32 -checkpoint=${checkpoint} ${1} ${2}"
fi
