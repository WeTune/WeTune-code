data_dir="${WETUNE_DATA_DIR:-wtune_data}"
issue_dir="issues"
result_dir='result'

gradle :testbed:run --args="IssueStudy"

cwd=$(pwd)
cd "${data_dir}/${issue_dir}" || exit

rm ${result_dir}
dir=$(ls -t -1 | grep 'run.\+' | head -1)
ln -sfr "${dir}" "${result_dir}"

cd "${cwd}" || exit