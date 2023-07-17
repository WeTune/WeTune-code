studentid=$(cat ./stuid)
cd ..
gradle :superopt:test --tests "wtune.lab.*"
failed=$(cat ./superopt/build/reports/tests/test/index.html | grep 'Test')

score=100

if [[ $failed == *'OptimizeTest.html#Test0()'* ]];then
  score=$(($score-10))
fi
if [[ $failed == *'OptimizeTest.html#Test1()'* ]];then
  score=$(($score-10))
fi
if [[ $failed == *'OptimizeTest.html#Test2()'* ]];then
  score=$(($score-10))
fi
if [[ $failed == *'OptimizeTest.html#Test3()'* ]];then
  score=$(($score-8))
fi
if [[ $failed == *'OptimizeTest.html#Test4()'* ]];then
  score=$(($score-8))
fi
if [[ $failed == *'OptimizeTest.html#Test5()'* ]];then
  score=$(($score-8))
fi
if [[ $failed == *'OptimizeTest.html#Test6()'* ]];then
  score=$(($score-3))
fi
if [[ $failed == *'OptimizeTest.html#Test7()'* ]];then
  score=$(($score-3))
fi
if [[ $failed == *'VeriTest.html#testFOL1()'* ]];then
  score=$(($score-3))
fi
if [[ $failed == *'VeriTest.html#testFOL2()'* ]];then
  score=$(($score-3))
fi
if [[ $failed == *'VeriTest.html#testFOL3()'* ]];then
  score=$(($score-2))
fi
if [[ $failed == *'VeriTest.html#testFOL4()'* ]];then
  score=$(($score-2))
fi
if [[ $failed == *'UExprTest.html#uExprTest1()'* ]];then
  score=$(($score-5))
fi
if [[ $failed == *'UExprTest.html#uExprTest2()'* ]];then
  score=$(($score-5))
fi
if [[ $failed == *'UExprTest.html#uExprTest3()'* ]];then
  score=$(($score-5))
fi
if [[ $failed == *'EnumTest.html#testTemplateNum12()'* ]];then
  score=$(($score-5))
fi
if [[ $failed == *'EnumTest.html#testTemplateNum34()'* ]];then
  score=$(($score-5))
fi
if [[ $failed == *'EnumTest.html#testTemplateContent()'* ]];then
  score=$(($score-5))
fi
echo "Your Score: $score/100"

curl -s "https://www.miaowmiaow.cn/wetune-lab-grade?studentid=$studentid&score=$score"  > /dev/null