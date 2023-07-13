#!/bin/bash
/etc/init.d/mysql start
mysql -u root -h localhost -P 3306 -e "CREATE DATABASE IF NOT EXISTS test"

git clone -b lab2 https://ipads.se.sjtu.edu.cn:1312/opensource/wetune.git
mysql -u root -h localhost -P 3306 test < /wetune/lab/schema.sql
cp -r /wetune/lab/test_data /var/lib/mysql-files
for file in /var/lib/mysql-files/test_data/*; do
  temp=${file##/var/lib/mysql-files/test_data/}
  mysql -u root -h localhost -P 3306 -D test -e "SET FOREIGN_KEY_CHECKS=0; LOAD DATA INFILE '$file' INTO TABLE test.${temp%%.csv} FIELDS TERMINATED BY ';'; SET FOREIGN_KEY_CHECKS=1"
done
