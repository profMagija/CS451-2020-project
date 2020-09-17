#!/bin/env python3
import sys
import subprocess
import time

import shutil
import os

NUM_PROC = int(sys.argv[1])
TARGET_TYPE = sys.argv[2]
NUM_PKT = int(sys.argv[3])
LANG = "java"

subprocess.run([f"./template_{LANG}/build.sh"], check=True)

subprocess.Popen(["python", "./barrier.py", "--processes", str(NUM_PROC)])

time.sleep(1)

shutil.rmtree("./outputs")
os.makedirs("./outputs")

with open("./hosts", "w") as hosts:
    for pn in range(1, NUM_PROC + 1):
        print(pn, "localhost", 11000 + pn, file=hosts)

procs = []

validator = None


def fifo_validator(name):
    expect = [1 for _ in range(NUM_PROC)]
    with open(name) as f:
        for line in f:
            parts = line.split()
            if parts[0] == 'd':
                sender = int(parts[1])
                seqnum = int(parts[2])

                if expect[sender - 1] == seqnum:
                    expect[sender - 1] += 1
                else:
                    yield f'{sender}:{seqnum} delivered before {sender}:{expect[sender - 1]}'
    for i, ex in enumerate(expect):
        if ex != NUM_PKT + 1:
            yield f'{i+1}:{ex} not delivered!'


if TARGET_TYPE == 'fifo':
    with open('./config', 'w') as conf_file:
        print(NUM_PKT, file=conf_file)
    validator = fifo_validator
else:
    print(' ?? wot')

for pn in range(1, NUM_PROC + 1):
    proc = subprocess.Popen([
        f"{os.environ['JAVA_HOME']}/bin/java",
        "-jar", "./template_java/bin/da_proc.jar",
        "--id", str(pn),
        "--hosts", "./hosts",
        "--barrier", "localhost:11000",
        "--output", f"./outputs/{pn}.out",
        "./config"
    ], shell=False)
    procs.append(proc)

for proc in procs:
    try:
        proc.wait(10000000000000)
    except KeyboardInterrupt:
        break

for proc in procs:
    try:
        print('proc is kill')
        proc.kill()
    except:
        pass

all_ok = True

for pn in range(1, NUM_PROC):
    for judgement in validator(f'./outputs/{pn}.out'):
        print(' !! oh noz [', pn, ']:', judgement)
        all_ok = False

if all_ok:
    print(' *** all ok ***')
