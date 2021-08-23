t=`date '+%m_%d_%H_%M'`

cp wtune_data/completed "wtune_data/completed.$t" || true
ag --nonumber '^\d+,\d+' wtune_data/enum.log >> wtune_data/completed || true

cp wtune_data/enum.out "wtune_data/enum.out.$t" || true

nohup gradle :superopt:run --args='runner.EnumSubstitution -parallel=true -echo -completed=wtune_data/completed 3 1' >wtune_data/enum.log 2>&1 </dev/null &
