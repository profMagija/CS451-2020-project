#!/bin/env python3
import sys
import subprocess
import time

import shutil
import os

NUM_PROC = int(sys.argv[1])
CONFIG_FILE = sys.argv[2]
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

# p=subprocess.Popen(your_command, preexec_fn=os.setsid)
# os.killpg(os.getpgid(p.pid), signal.SIGTERM)

for pn in range(1, NUM_PROC + 1):
    proc = subprocess.Popen([
        f"{os.environ['JAVA_HOME']}/bin/java",
        "-jar", "./template_java/bin/da_proc.jar",
        "--id", str(pn),
        "--hosts", "./hosts",
        "--barrier", "localhost:11000",
        "--output", f"./outputs/{pn}.out",
        CONFIG_FILE
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
