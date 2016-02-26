#!/bin/bash

base=${0%/*};
SOLR_HOME=$base/solr-conf/biosamples

DEBUG_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1044"

while getopts ":m:p:h" opt; do
  case ${opt} in
    m )
      mem=$OPTARG
      ;;
    p )
      port=$OPTARG
      ;;
    h)
      echo "Usage: solr-start [-m] [-p]"
      echo "    -m  Specify memory requirements for Solr"
      echo "    -p  Specify port on which to run Solr"
      exit 0;
      ;;
    \? )
      echo "Invalid option: $OPTARG"
      exit 1;
      ;;
    : )
      echo "Invalid option: $OPTARG requires an argument"
      ;;
  esac
done

# check SOLR_DIR has been set
if [ -z $SOLR_DIR ]
  then
    echo '$SOLR_DIR not set - please set this to the location of your Solr installation' >&2
    exit 1;
  else
    if [ ${mem} ]
      then
        if [ ${port} ]
          then
            echo "Starting Solr using $SOLR_HOME, mem: $mem and port: $port"
            $SOLR_DIR/bin/solr start -s $SOLR_HOME -m $mem -p $port
          else
            echo "Starting Solr using $SOLR_HOME, mem: $mem"
            $SOLR_DIR/bin/solr start -s $SOLR_HOME -m $mem
        fi
      else
        echo "Starting Solr using $SOLR_HOME"
        $SOLR_DIR/bin/solr start -s $SOLR_HOME
    fi
fi