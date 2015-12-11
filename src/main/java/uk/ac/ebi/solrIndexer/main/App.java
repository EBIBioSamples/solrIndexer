package uk.ac.ebi.solrIndexer.main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.solrIndexer.properties.PropertiesManager;

public class App {
	private static Logger log = LoggerFactory.getLogger(App.class.getName());
	
	public static void main( String[] args ) {
		log.info("Entering application.");
	
		Collection<SolrInputDocument> docs = null;
	
		ConcurrentUpdateSolrClient client = new ConcurrentUpdateSolrClient(PropertiesManager.getSolrCorePath(), 10, 8);
		client.setParser(new XMLResponseParser());
	
		try {
			docs = new ArrayList<SolrInputDocument>();
			int offset;
	
			/* -- Handle Groups -- */
			log.info("Handling Groups");
			offset = 0;
	
			List<BioSampleGroup> groups;
			while ( (groups = DataBaseManager.getAllIterableGroups(offset, 10000)).size() > 0) {
	
				for (BioSampleGroup group : groups) {
					SolrInputDocument document = SolrManager.generateBioSampleGroupSolrDocument(group);
	
					if (document != null) {
	    				docs.add(document);
	
	    				if (docs.size() > 9999) {
	    					UpdateResponse response = client.add(docs, 30000);
	    					if (response.getStatus() != 0) {
	    						log.error("Indexing groups error: " + response.getStatus());
	    					}
	    					docs.clear();
	    				}
					}
				}
	
				offset += groups.size();
			}
	
			DataBaseManager.closeConnection();
			log.info("Group documents generated.");
			/* -------------------- */
	
			/* -- Handle Samples -- */
			log.info("Handling Samples");
			offset = 0;
	
			List<BioSample> samples;
			while ( (samples = DataBaseManager.getAllIterableSamples(offset, 10000)).size() > 0) {
				
				for (BioSample sample : samples) {
					SolrInputDocument document = SolrManager.generateBioSampleSolrDocument(sample);
	
					if (document != null) {
	    				docs.add(document);
	
	    				if (docs.size() > 9999) {
	    					UpdateResponse response = client.add(docs, 30000);
	    					if (response.getStatus() != 0) {
	    						log.error("Indexing samples error: " + response.getStatus());
	    					}
	    					docs.clear();
	    				}
					}
				}
	
				offset += samples.size();
			}
			log.info("Sample documents generated.");
			/* -------------------- */
	
			log.info("Indexing finished!");
	
		} catch (Exception e) {
			log.error("Error creating index", e);
	
		} finally {
	
	    	try {
	        	if (docs.size() > 0) {
	        		client.add(docs);
	        		client.commit();
				}
				docs.clear();
	
			} catch (SolrServerException | IOException e) {
				log.error("Finally - Error creating index", e);
			}
	
			DataBaseManager.closeConnection();
			client.close();
	
	    	System.exit(0);
		}
	
	}

}
