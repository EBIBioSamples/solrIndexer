#!/bin/bash
set -e

#read environmental config from here...
source "$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"/load_env.sh

#clean any existing content
rm -rf $NEO_DATA/graph.db.tmp

#combine multiple csv files and make sure they have unique lines
#do this in parallel for all files at once
sort -u -i -o $IMPORTER/output/sample.csv $IMPORTER/output/sample.*.csv &
sort -u -i -o $IMPORTER/output/group.csv $IMPORTER/output/group.*.csv &
sort -u -i -o $IMPORTER/output/links.csv $IMPORTER/output/links.*.csv &
sort -u -i -o $IMPORTER/output/membership.csv $IMPORTER/output/membership.*.csv &
sort -u -i -o $IMPORTER/output/derivation.csv $IMPORTER/output/derivation.*.csv &
sort -u -i -o $IMPORTER/output/childof.csv $IMPORTER/output/childof.*.csv &
sort -u -i -o $IMPORTER/output/recuratedfrom.csv $IMPORTER/output/recuratedfrom.*.csv &
sort -u -i -o $IMPORTER/output/sameas.csv $IMPORTER/output/sameas.*.csv &
sort -u -i -o $IMPORTER/output/haslink_group.csv $IMPORTER/output/haslink_group.*.csv  &
sort -u -i -o $IMPORTER/output/haslink_sample.csv $IMPORTER/output/haslink_sample.*.csv & 
wait

#create new content
time nice $NEO4J_BIN/neo4j-import --bad-tolerance 10000 --into $NEO_DATA/graph.db.tmp --i-type string \
	--nodes:Sample "$IMPORTER/csv/sample_header.csv,`ls -1 $IMPORTER/output/sample.csv | paste -sd ,`" \
	--nodes:Group "$IMPORTER/csv/group_header.csv,`ls -1 $IMPORTER/output/group.csv | paste -sd ,`" \
	--nodes:ExternalLink "$IMPORTER/csv/links_header.csv,`ls -1 $IMPORTER/output/links.csv | paste -sd ,`" \
	--relationships:MEMBERSHIP "$IMPORTER/csv/membership_header.csv,`ls -1 $IMPORTER/output/membership.csv | paste -sd ,`" \
	--relationships:DERIVATION "$IMPORTER/csv/derivation_header.csv,`ls -1 $IMPORTER/output/derivation.csv | paste -sd ,`" \
	--relationships:CHILDOF "$IMPORTER/csv/childof_header.csv,`ls -1 $IMPORTER/output/childof.csv | paste -sd ,`" \
	--relationships:RECURATION "$IMPORTER/csv/recuratedfrom_header.csv,`ls -1 $IMPORTER/output/recuratedfrom.csv | paste -sd ,`" \
	--relationships:SAMEAS "$IMPORTER/csv/sameas_header.csv,`ls -1 $IMPORTER/output/sameas.csv | paste -sd ,`" \
	--relationships:HASLINK "$IMPORTER/csv/haslink_group_header.csv,`ls -1 $IMPORTER/output/haslink_group.csv | paste -sd ,`" \
	--relationships:HASLINK "$IMPORTER/csv/haslink_sample_header.csv,`ls -1 $IMPORTER/output/haslink_sample.csv | paste -sd ,`" 
	
	
#Indexes are not created during the import. Instead you’ll need to add indexes afterwards
       
#create indexes
echo "Creating indexes..."
time nice $NEO4J_BIN/neo4j-shell -path $NEO_DATA/graph.db.tmp -file $IMPORTER/indexes.cypher

#replace graph 
#rm -rf $NEO_DATA/graph.db
#mv $NEO_DATA/graph.db.tmp $NEO_DATA/graph.db
    
echo "All Done!"
    
