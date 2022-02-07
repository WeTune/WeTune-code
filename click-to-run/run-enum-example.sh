#!/bin/bash

dump='false'

# read arguments
while [[ $# -gt 0 ]]; do
  case $1 in
  -dump)
    dump='true'
    shift 1
    ;;
  *)
    index="${2}"
    shift 2
    ;;
  esac
done

gradle :superopt:run --args="RunEnumExample -dump=${dump} -I=${index}"
