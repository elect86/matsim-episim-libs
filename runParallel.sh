#!/bin/bash

#export JAVA_OPTS='-Xms90G -Xmx90G -XX:+UseParallelGC'
export JAVA_OPTS='-Xms10G -Xmx10G -XX:+UseParallelGC'

export EPISIM_SETUP='org.matsim.run.batch.DresdenCalibration'
export EPISIM_PARAMS='org.matsim.run.batch.DresdenCalibration$Params'

export EPISIM_INPUT='dresden'
#'/scratch/projects/bzz0020/episim-input'
export EPISIM_OUTPUT='output-dresden'


classpath="build/libs/matsim-episim-*-all.jar"

echo "***"
echo "classpath: $classpath"
echo "***"

# main
main="org.matsim.run.RunParallel"

arguments="--tasks 1 --total-worker 1 --worker-index 0 --iterations 750"
command="java -cp $classpath $JAVA_OPTS -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector $main $arguments"
echo "command is $command"
#perf mem record -F 10 -g -o perf-${SLURM_JOB_ID}-$(( SLURM_ARRAY_TASK_ID-1 )).data -- $command
$command
#pgrep -af java
