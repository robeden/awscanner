#!/bin/sh

cat $1 | grep "====\|InstanceInfo" | grep -v "public_ip=null" | grep -o "====.*====\|public_ip=[0-9.]*" | grep -o  "====.*====\|[0-9.]*"
