echo "请输入学号："
read studentid
result=$(curl -s "https://www.miaowmiaow.cn/wetune-lab-reg?studentid=$studentid")
echo $result
if [[ $result == *'error'* ]];then
  exit
fi
echo $result > ./token
echo $studentid > ./studentId