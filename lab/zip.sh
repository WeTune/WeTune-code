cd ..
gradle clean
studentId=$(cat ./studentId)
zip -r $studentId.zip superopt
