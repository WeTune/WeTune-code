##### volume mapping ######
HOST_DUMP_PATH=/data/dump
HOST_MSSQL_PATH=/data/mssql
DOCKER_DUMP_PATH=/home/root/wetune/wtune_data/dump
DOCKER_MSSQL_PATH=/var/opt/mssql

##### set up sqlserver ######
docker pull mcr.microsoft.com/mssql/server:2019-latest
docker rm mssql -f
docker run -e "ACCEPT_EULA=Y" -e MSSQL_PID='Developer' -e "MSSQL_SA_PASSWORD=mssql2019Admin" -u root -p 1433:1433 --name mssql -v $HOST_MSSQL_PATH:$DOCKER_MSSQL_PATH -v $HOST_DUMP_PATH:$DOCKER_DUMP_PATH -d mcr.microsoft.com/mssql/server:2019-latest

##### set up wetune #######
docker build -t wetune:0.1 .
docker rm wetune -f
docker run -d -it --name wetune -v $HOST_DUMP_PATH:$DOCKER_DUMP_PATH --network=host --privileged=true wetune:0.1

########## Set Directories ########
repo_dir='/home/root/wetune'

######### Clone Repository ################
docker exec wetune apt-get -y update
docker exec wetune apt-get -y upgrade
docker exec wetune apt-get install -y git
docker exec wetune git clone https://ipads.se.sjtu.edu.cn:1312/opensource/wetune.git /temp
docker exec wetune mv /temp/.git /home/root/wetune
docker exec wetune rm -rf /temp
docker exec wetune bash -c "cd ${repo_dir} && git reset --hard HEAD"

######### download dependencies and compile sub-projects #####
docker exec wetune bash -c "cd ${repo_dir} && gradle compileJava"

######## result directory in host machine ##########
result_dir='issue_result_from_docker'
sudo mkdir ${result_dir}

# Issue Study #
docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/make-db.sh -tag base"
docker exec wetune bash -c \
  "cd ${repo_dir} && bash click-to-run/generate-data.sh -target diaspora,discourse,lobsters,gitlab,redmine,solidus,spree -tag base"
docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/issue-study.sh"

######## copy result from docker container to host machine ##########
docker cp wetune:/home/root/wetune/wtune_data/issues ./${result_dir}


docker exec -it wetune /bin/bash
