package uk.ac.ebi.solrIndexer.main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;

public class App {
	private static Logger log = LoggerFactory.getLogger(App.class.getName());

    public static void main( String[] args ) {
    	log.info("Entering application.");

    	DataBaseConnection dbc = null;
    	Collection<SolrInputDocument> docs = null;

    	try {
    		dbc = DataBaseConnection.getInstance();
    		docs = new ArrayList<SolrInputDocument>();

    		/* -- Handle Groups -- 
    		List<BioSampleGroup> groups = BioSDEntities.fetchGroups(dbc);
    		if (groups != null && !groups.isEmpty()) {

    			log.info("Generating Solr group documents");
    			//BioSampleGroupXMLService xmlService = new BioSampleGroupXMLService();
        		for (BioSampleGroup bsg : groups) {
        			SolrInputDocument document = SolrIndexer.generateBioSampleGroupSolrDocument(bsg);
        			if (document != null) {
        				docs.add(document);
        			}

        			if (docs.size() > 1000) {
        				UpdateResponse response = SolrIndexer.getInstance().getConcurrentUpdateSolrClient().add(docs);
        				if (response.getStatus() != 0) {
        					log.error("Indexing groups error: " + response.getStatus());
        				}
        				docs.clear();
        			} 
        		}

    		}
*/
			/* -- Handle Samples -- */
    		List<BioSample> samples = BioSDEntities.fetchSamples(dbc);
    		if (samples != null && !samples.isEmpty()) {
    			log.info("Generating Solr sample documents");
    			//BioSampleXMLService xmlService = new BioSampleXMLService();
        		for (BioSample bs : samples) {
        			SolrInputDocument document = SolrIndexer.generateBioSampleSolrDocument(bs);
        			if (document != null) {
        				docs.add(document);
        			}

        			if (docs.size() > 1000) {
        				UpdateResponse response = SolrIndexer.getInstance().getConcurrentUpdateSolrClient().add(docs);
        				if (response.getStatus() != 0) {
        					log.error("Indexing groups error: " + response.getStatus());
        				}
        				docs.clear();
        				break;
        			}

        		}
        		
    		} else {
    			log.debug("No samples to index.");
    		}

    		log.info("Indexing finished!");

    	} catch (Exception e) {
    		log.error("Error creating index", e);

    	} finally {
        	if (dbc.getEntityManager()!= null && dbc.getEntityManager().isOpen()) {
        		dbc.getEntityManager().close();
    		}

        	try {
            	if (docs.size() > 0) {
            		SolrIndexer.getInstance().getConcurrentUpdateSolrClient().add(docs, 300000);
    			}
				SolrIndexer.getInstance().getConcurrentUpdateSolrClient().commit();
			} catch (SolrServerException | IOException e) {
				log.error("Error creating index", e);
			}

        	System.exit(0);
    	}

    }

}
