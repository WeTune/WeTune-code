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

mysql -u root -h localhost -P 3306 -e "SET GLOBAL optimizer_switch='index_merge=off,index_merge_union=off,index_merge_sort_union=off,index_merge_intersection=off,engine_condition_pushdown=off,index_condition_pushdown=off,mrr=off,mrr_cost_based=off,block_nested_loop=off,batched_key_access=off,materialization=off,semijoin=off,loosescan=off,firstmatch=off,duplicateweedout=off,subquery_materialization_cost_based=off,use_index_extensions=off,condition_fanout_filter=off,derived_merge=off,use_invisible_indexes=off,skip_scan=off,hash_join=off,subquery_to_derived=off,prefer_ordering_index=off,hypergraph_optimizer=off,derived_condition_pushdown=off';"
mysql -u root -h localhost -P 3306 -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '123456';"

# set user and password of the container lab
username="root"
new_password="123456"
echo "$username:$new_password" | chpasswd

sed -i 's/#PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config
sed -i 's/#PasswordAuthentication yes/PasswordAuthentication yes/' /etc/ssh/sshd_config
service ssh restart