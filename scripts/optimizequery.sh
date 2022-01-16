gradle :superopt:run --args="runner.OptimizeQuery"
cd 'wtune_data/opt' || exit
dir=$(ls -t -1 | ag 'run.+' | head -1)
ln -sfr "${dir}" 'result'
