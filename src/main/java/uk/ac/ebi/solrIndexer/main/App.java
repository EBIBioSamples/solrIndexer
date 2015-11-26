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
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;

public class App {
	private static Logger log = LoggerFactory.getLogger(App.class.getName());

    public static void main( String[] args ) {
    	log.info("Entering application.");

    	Collection<SolrInputDocument> docs = null;

    	try {
    		docs = new ArrayList<SolrInputDocument>();

    		/* -- Handle Groups -- */
    		List<BioSampleGroup> groups = DataBaseManager.fetchGroups();
    		if (groups != null && !groups.isEmpty()) {
    			log.info("[" + groups.size() + "]" + " groups fetched.");

        		for (BioSampleGroup bsg : groups) {
        			SolrInputDocument document = SolrManager.generateBioSampleGroupSolrDocument(bsg);
        			if (document != null) {
        				docs.add(document);
        			}

        			checkDocsSize(docs); 
        		}
        		log.info("Group documents generated.");
    		}

			/* -- Handle Samples -- */
    		List<String> submissions = DataBaseManager.fetchSubmissionsAccessions();
    		if (submissions != null && !submissions.isEmpty()) {
    			log.info("[" + submissions.size() + "]" + " submissions accessions fetched.");

    			for (String acc : submissions) {

    				for (BioSample sample : DataBaseManager.fetchSubmission(acc).getSamples()) {

    					SolrInputDocument document = SolrManager.generateBioSampleSolrDocument(sample);
        				if (document != null) {
            				docs.add(document);
            			}

        				checkDocsSize(docs);
    				}
            		log.info("Sample documents generated.");
    			}

    		} else {
    			log.debug("No samples to index.");
    		}

    		log.info("Indexing finished!");

    	} catch (Exception e) {
    		log.error("Error creating index", e);

    	} finally {

    		DataBaseManager.closeConnection();

        	try {
            	if (docs.size() > 0) {
            		SolrManager.getInstance().getConcurrentUpdateSolrClient().add(docs, 300000);
    			}
				SolrManager.getInstance().getConcurrentUpdateSolrClient().commit();
			} catch (SolrServerException | IOException e) {
				log.error("Error creating index", e);
			}

        	System.exit(0);
    	}

    }

	/**
	 * @param docs
	 * @throws SolrServerException
	 * @throws IOException
	 */
	private static void checkDocsSize(Collection<SolrInputDocument> docs) throws SolrServerException, IOException {
		if (docs.size() > 1000) {
			UpdateResponse response = SolrManager.getInstance().getConcurrentUpdateSolrClient().add(docs);
			if (response.getStatus() != 0) {
				log.error("Indexing groups error: " + response.getStatus());
			}
			docs.clear();
		}
	}

}
