#!/bin/bash
javaCmd="java"
regminer="regs4j.jar"
# Check if at least two parameters were passed
if [ $# -lt 1 ]
  then
    echo "Please provide at least one parameters"
    exit 1
fi
 params=""
for param in "${@:1}"
do
    params=$params" "$param
done

$javaCmd -jar $regminer $params