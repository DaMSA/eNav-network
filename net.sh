#!/bin/bash

version=`mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | sed -n -e '/^\[.*\]/ !{ /^[0-9]/ { p; q } }'`


if [ "$1" = "server" ]; then
   java -jar dma-network-server-distribution/target/dma-network-server-distribution-$version.jar
  exit
else
	echo Unknown target: "$1"
	echo Valid targets are:

fi

echo "  server           Runs the server"

