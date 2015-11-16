package uk.ac.ebi.solrIndexer.main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.solrIndexer.service.BioSampleGroupXMLService;

public class App {
	private static Logger log = LoggerFactory.getLogger(App.class.getName());

    public static void main( String[] args ) {
    	log.info("Entering application.");

    	DataBaseConnection dbc = null;
    	Collection<SolrInputDocument> docs = null;

    	try {
    		dbc = DataBaseConnection.getInstance();
    		docs = new ArrayList<SolrInputDocument>();

    		List<BioSampleGroup> groups = BioSDEntities.fetchGroups(dbc);
    		if (groups != null && !groups.isEmpty()) {

    			log.info("Generating Solr group documents");
    			BioSampleGroupXMLService xmlService = new BioSampleGroupXMLService();
        		for (BioSampleGroup bsg : groups) {
        			log.info("---xml: " + xmlService.getXMLString(bsg));
        			break;
        			/*
        			docs.add(SolrIndexer.generateBioSampleGroupSolrDocument(bsg));

        			if (docs.size() > 1000) {
        				
        				UpdateResponse response = SolrIndexer.getInstance().getConcurrentUpdateSolrClient().add(docs);
        				if (response.getStatus() != 0) {
        					log.error("Indexing groups error: " + response.getStatus());
        				}
        				docs.clear();
        				
        			} 
        			 */
        		}
    		}
/*
    		List<BioSample> samples = BioSDEntities.fetchSamples(dbc);
    		if (samples != null && !samples.isEmpty()) {
    			log.info("Generating Solr sample documents");
    			int i = 1;
        		for (BioSample bs : samples) {
        			log.info("---Sample ACC: " + bs.getAcc());
        			i++;
        			if (i == 11) break;
        			
        			docs.add(SolrIndexer.generateBioSampleSolrDocument(bs)); //TODO handle null
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
    	} catch (Exception e) {
    		log.error("Error creating index", e);
    		e.printStackTrace();
    	} finally {
        	if (dbc.getEntityManager()!= null && dbc.getEntityManager().isOpen()) {
        		dbc.getEntityManager().close();
    		}
/*
        	try {
            	if (docs.size() > 0) {
            		SolrIndexer.getInstance().getConcurrentUpdateSolrClient().add(docs, 300000);
    			}
				SolrIndexer.getInstance().getConcurrentUpdateSolrClient().commit();
			} catch (SolrServerException | IOException e) {
				log.error("Error creating index", e);
			}
*/
        	System.exit(0);
    	}

    }

}
