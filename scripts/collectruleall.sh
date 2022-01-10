#!/bin/bash

set -eu -o pipefail

username='zhouz'
prefix='10.0.0.10'
default_ips=(2 3 4 5)

while [[ $# -gt 0 ]]; do
    case "$1" in
        --prefix)
            prefix="$2"
            shift 2
            ;;
        --user)
            username="$2"
            shift 2
            ;;
        *)
            ips+=("$1")
            shift
    esac
done
ips=(${ips[@]:-${default_ips[@]}})

cd wtune_data

t=$(date '+%m_%d_%H_%M')
dirname="all_rules/${t}"
mkdir -p "${dirname}" 2>/dev/null || true

for var in "${ips[@]}"; do
    ip="${prefix}${var}"
    ssh "${username}@${ip}" 'cd projects/wtune-code; scripts/collectrule.sh'
    scp "${username}@${ip}:~/projects/wtune-code/wtune_data/rules.partial.txt" "${dirname}/rules.${ip}.txt"
    cat "${dirname}/rules.${ip}.txt" >> "${dirname}/rules.txt"
done

ln -sfr "${dirname}/rules.txt" 'rules.txt'