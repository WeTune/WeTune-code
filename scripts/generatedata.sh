gradle :testbed:run --args="Generate"
cd 'wtune_data/opt' || exit
dir=$(ls -t -1 | ag 'run.+' | head -1)
ln -sfr "${dir}" 'result'
