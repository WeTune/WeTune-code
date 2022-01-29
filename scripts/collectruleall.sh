#!/bin/bash

set -eu -o pipefail

data_dir="${WETUNE_DATA_DIR:-wtune_data}"
rule_dir='rules'

username='zhouz'
prefix='10.0.0.10'
default_ips=(2 3 4 5)
wetune_path='projects/wtune-code'

while [[ $# -gt 0 ]]; do
  case "$1" in
  -prefix)
    prefix="$2"
    shift 2
    ;;
  -user)
    username="$2"
    shift 2
    ;;
  -path)
    wetune_path="$2"
    shift 2
    ;;
  *)
    ips+=("$1")
    shift
    ;;
  esac
done
ips=(${ips[@]:-${default_ips[@]}})

mkdir -p "${data_dir}/${rule_dir}"
cd "${data_dir}/${rule_dir}"
rule_file="rules.raw.txt"

if [ -f "${rule_file}" ]; then
  truncate -s0 "${rule_file}"
fi

for var in "${ips[@]}"; do
  ip="${prefix}${var}"
  ssh "${username}@${ip}" "cd ${wetune_path}; scripts/collectrule.sh"
  scp "${username}@${ip}:${wetune_path}/${data_dir}/${rule_dir}/rules.local.txt" "rules.${ip}.txt"
  cat "rules.${ip}.txt" >>"${rule_file}"
done
