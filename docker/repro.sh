docker build -t wetune:0.1 .

docker run --rm -d -it --name wetune --privileged=true wetune:0.1 /sbin/init

########## Set Directories ########
repo_dir='/home/root/wetune'

######### Clone Repository ################
docker exec wetune apt-get -y update && apt-get -y upgrade
docker exec wetune apt-get install -y git
docker exec wetune git clone https://ipads.se.sjtu.edu.cn:1312/opensource/wetune.git $repo_dir

########## Set Up SqlServer ##########
docker exec wetune apt-get install -y sudo
docker exec wetune sudo MSSQL_SA_PASSWORD=mssql2019Admin \
                  MSSQL_PID=developer \
                  /opt/mssql/bin/mssql-conf -n setup accept-eula

######### choose to whether to run discovery.sh ########
read -r -p "Do you want to run discovery.sh to find rules which takes about 3600 CPU hours in our machine? [Y/n] " input

case $input in
    [yY][eE][sS]|[yY])
    echo "you choose yes"
		docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/discover-rules.sh"
    docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/loop-until-discover-end.sh"
		docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/collect-rules.sh && click-to-run/reduce-rules.sh"
		docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/show-progress.sh"
		;;

    [nN][oO]|[nN])
		echo "you choose no"
    ;;

    *)
		echo "Invalid input..."
		exit 1
		;;
esac

######## wetune: rewrite queries && pick one with the minimal cost#########
docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/rewrite-queries.sh"
docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/prepare-workload.sh -tag base"
docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/estimate-cost.sh"

######## prepare workload in sqlserver #########
docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/prepare-workload.sh -tag zipf"
docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/prepare-workload.sh -tag large"
docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/prepare-workload.sh -tag large_zipf"

######## wetune: profile the performance of rewritten queries ##########
docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/profile-cost.sh -tag base &&
                                              bash click-to-run/profile-cost.sh -tag zipf &&
                                              bash click-to-run/profile-cost.sh -tag large &&
                                              bash click-to-run/profile-cost.sh -tag large_zipf"

######## view rewriting and profiling results of both wetune #########
docker exec wetune bash -c "cd ${repo_dir} && bash click-to-run/view-all.sh"

######## copy result from docker container to host machine ##########
sudo mkdir "result_from_docker"
docker cp wetune:/home/root/wetune/wtune_data/rewrite ./result_from_docker
docker cp wetune:/home/root/wetune/wtune_data/profile ./result_from_docker
docker cp wetune:/home/root/wetune/wtune_data/viewall ./result_from_docker
docker cp wetune:/home/root/wetune/wtune_data/enumeration ./result_from_docker
docker cp wetune:/home/root/wetune/wtune_data/rules/rules.txt ./result_from_docker

docker exec -it wetune /bin/bash
