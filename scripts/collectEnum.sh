cwd=$PWD

t=`date '+%m_%d_%H_%M'`
dir="enum_results.$t"

mkdir $dir
cd $dir
mkdir "cube2" && scp zhouz@10.0.0.102:~/projects/wtune-code/wtune_data/enum.out* ./cube2
mkdir "cube4" && scp zhouz@10.0.0.104:~/projects/wtune-code/wtune_data/enum.out* ./cube4
mkdir "cube5" && scp zhouz@10.0.0.105:~/projects/wtune-code/wtune_data/enum.out* ./cube5

cat \
    cube2/enum.out* \
    cube4/enum.out* \
    cube5/enum.out* > substitutions.raw

cd "$cwd"
rm enum_results
ln -s "$dir" enum_results
