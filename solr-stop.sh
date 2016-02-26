#!/bin/bash

while getopts ":p:h" opt; do
  case ${opt} in
    p )
      port=$OPTARG
      ;;
    h)
      echo "Usage: solr-stop [-p]"
      echo "    -p  Specify port of Solr instance to stop (defaults to 8983)"
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
    exit 1
  else
    if [ ${port} ]
      then
        echo "Stopping Solr using SOLR_DIR $SOLR_DIR, port $port"
        $SOLR_DIR/bin/solr stop -p $port
      else
        echo "Stopping Solr using SOLR_DIR $SOLR_DIR"
        $SOLR_DIR/bin/solr stop
    fi
fi
