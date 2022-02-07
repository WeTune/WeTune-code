#! /bin/bash

data_dir="${WETUNE_DATA_DIR:-wtune_data}"
profile_dir="profile"
tag="base"
optimizer="WeTune"

while [[ $# -gt 0 ]]; do
  case "$1" in
  "-tag")
    tag="${2}"
    shift 2
    ;;
  "-opt" | "-optimizer")
    optimizer="${2}"
    shift 2
    ;;
  *)
    positional_args+=("${1}")
    shift
    ;;
  esac
done

gradle :testbed:run --args="Profile -dir=${profile_dir} -tag=${tag} -optimizer=${optimizer} "

