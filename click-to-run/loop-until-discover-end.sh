sleep 60
process=`jps -l | grep 'superopt.Entry' | wc -l`
while [ $process -gt 0 ] ; do
  echo "$process processes are discovering rules, current time is" $(date +%H:%M:%S)
  sleep 60
  process=`jps -l | grep 'superopt.Entry' | wc -l`
done
