show_one() {
    x=$(ssh zhouz@$1 "cd projects/wtune-code && ag --nonumber '^\d+,\d+' wtune_data/enum.log | wc -l")
    y=$(ssh zhouz@$1 "cd projects/wtune-code && wc -l wtune_data/completed | cut -f1 -d' '")

    echo $(( $x + $y ))
}

cube2=$(show_one '10.0.0.102')
# $cube3=$(show_one '10.0.0.103')
cube4=$(show_one '10.0.0.104')
cube5=$(show_one '10.0.0.105')

echo "cube2 $cube2"
# echo "cube3 $cube3"
echo "cube4 $cube4"
echo "cube5 $cube5"
echo "total $(( $cube2 + $cube4 + $cube5))"
