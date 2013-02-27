#!/bin/bash

if [ "$1" = "server" ]; then
   java -jar dma-network-server-distribution/target/dma-network-server-distribution-0.1-SNAPSHOT.jar
  exit
else
	echo Unknown target: "$1"
	echo Valid targets are:

fi

echo "  server           Runs the server"

