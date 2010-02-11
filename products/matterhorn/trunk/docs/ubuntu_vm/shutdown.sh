#!/bin/bash

#
# Stop felix
#
# Kill the currently running server (there's gotta be a better way!)
MATTERHORN_PID=`ps aux | awk '/felix.jar/ && !/awk/ {print $2}'`
if [ -z $MATTERHORN_PID ]; then
  echo "Matterhorn already stopped"
  exit 1
fi

kill $MATTERHORN_PID
