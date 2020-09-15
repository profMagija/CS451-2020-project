#!/bin/bash

NUM_PROC=$1
CONFIG_FILE=$2
L=java

./template_$L/build.sh || exit 1

python ./barrier.py --processes $NUM_PROC &

sleep 1

mkdir -p ./outputs

rm -f ./outputs/*.out
rm -f ./hosts

for pn in $(seq 1 $NUM_PROC)
do
    echo $pn localhost $((11000 + $pn)) >> ./hosts
done

for pn in $(seq 1 $NUM_PROC)
do
    ./template_$L/run.sh --id $pn --hosts ./hosts --barrier localhost:11000 --output ./outputs/$pn.out $CONFIG_FILE &
done

