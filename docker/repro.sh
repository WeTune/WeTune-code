docker build -t wetune:0.1 .

docker run --rm -d -it --name wetune wetune:0.1

repo_dir='/home/root/wetune'
docker exec wetune apt-get update
docker exec wetune apt-get install -y git
docker exec wetune git clone https://ipads.se.sjtu.edu.cn:1312/opensource/wetune.git $repo_dir

docker exec -it wetune /bin/bash