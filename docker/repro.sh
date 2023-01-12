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
sudo mkdir "result_from_docker"

######### choose to whether to run discovery.sh ########
read -r -p "Do you want to run discovery.sh to find rules which takes about 3600 CPU hours in our machine? [Y/n] " input

case $input in
    [yY][eE][sS]|[yY])
    echo "you choose yes"
    docker exec wetune bash -c "cd ${repo_dir} && git checkout rd"
    docker exec wetune bash -c "cd ${repo_dir} && git add sql/ && git reset --hard HEAD"
    docker exec wetune bash -c "cd ${repo_dir} && gradle compileJava"
		docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/discover-rules.sh"
    docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/loop-until-discover-end.sh"
		docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/collect-rules.sh"
		docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/reduce-rules.sh"
		docker cp wetune:/home/root/wetune/wtune_data/enumeration ./result_from_docker
    docker cp wetune:/home/root/wetune/wtune_data/rules/rules.txt ./result_from_docker
		docker exec wetune bash -c "cd ${repo_dir} && git checkout main"
		docker exec wetune bash -c "cd ${repo_dir} && git add sql/ && git reset --hard HEAD"
    docker exec wetune bash -c "cd ${repo_dir} && gradle compileJava"
		;;

    [nN][oO]|[nN])
		echo "you choose no"
    ;;

    *)
		echo "Invalid input..."
		exit 1
		;;
esac

# calcite #
###### rewrite queries && pick one with the minimal cost #####
docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/rewrite-queries.sh -calcite"
docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/prepare-workload.sh -calcite -tag base"
docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/estimate-cost.sh -calcite"

##### profile the performance of rewritten queries #####
docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/profile-cost.sh -calcite -tag base"

#### view result of calcite #####
docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/view-all.sh -calcite"


# wetune #
######## wetune: rewrite queries && pick one with the minimal cost#########
docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/rewrite-queries.sh"
docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/prepare-workload.sh -tag base"
docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/estimate-cost.sh"

######## prepare workload in sqlserver #########
docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/prepare-workload.sh -tag zipf"
docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/prepare-workload.sh -tag large"
docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/prepare-workload.sh -tag large_zipf"

######## wetune: profile the performance of rewritten queries ##########
docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/profile-cost.sh -tag base"
docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/profile-cost.sh -tag zipf"
docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/profile-cost.sh -tag large"
docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/profile-cost.sh -tag large_zipf"

######## view rewriting and profiling results of wetune #########
docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/view-all.sh"
docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/view-all.sh -all"

######## copy result from docker container to host machine ##########
docker cp wetune:/home/root/wetune/wtune_data/calcite ./result_from_docker
docker cp wetune:/home/root/wetune/wtune_data/profile_calcite ./result_from_docker
docker cp wetune:/home/root/wetune/wtune_data/viewall_calcite ./result_from_docker

docker cp wetune:/home/root/wetune/wtune_data/rewrite ./result_from_docker
docker cp wetune:/home/root/wetune/wtune_data/profile ./result_from_docker
docker cp wetune:/home/root/wetune/wtune_data/viewall ./result_from_docker

docker cp wetune:/home/root/wetune/wtune_data/viewall_statistics ./result_from_docker

docker exec -it wetune /bin/bash
