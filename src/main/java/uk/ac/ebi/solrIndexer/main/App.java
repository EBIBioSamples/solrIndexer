package uk.ac.ebi.solrIndexer.main;

import java.io.*;
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
import uk.ac.ebi.solrIndexer.service.xml.BioSampleGroupXMLService;
import uk.ac.ebi.solrIndexer.service.xml.XMLService;

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
    			log.info("[" + groups.size() + "]" + "groups fetched.");


        		for (BioSampleGroup bsg : groups) {
					BioSampleGroupXMLService xmlDocCreator = new BioSampleGroupXMLService();
					String xmlString = xmlDocCreator.getXMLString(bsg);


					PrintWriter pw = new PrintWriter("./TestFiles/biosample-test.xml","UTF-8");

					pw.write(xmlString);
					pw.close();

					log.info("\n" + xmlString);

					break;

//        			SolrInputDocument document = SolrManager.generateBioSampleGroupSolrDocument(bsg);
//        			if (document != null) {
//        				docs.add(document);
//        			}
//
//        			if (docs.size() > 1000) {
//        				UpdateResponse response = SolrManager.getInstance().getConcurrentUpdateSolrClient().add(docs);
//        				if (response.getStatus() != 0) {
//        					log.error("Indexing groups error: " + response.getStatus());
//        				}
//        				docs.clear();
//        			}
        		}

    		}

			/* -- Handle Samples -- */
			/*
    		List<String> submissions = DataBaseManager.fetchSubmissionsAccessions();
    		if (submissions != null && !submissions.isEmpty()) {
    			log.info("[" + submissions.size() + "]" + " submissions accessions fetched.");

    			for (String acc : submissions) {

    				for (BioSample sample : DataBaseManager.fetchSubmission(acc).getSamples()) {

    					SolrInputDocument document = SolrManager.generateBioSampleSolrDocument(sample);
        				if (document != null) {
            				docs.add(document);
            			}

        				if (docs.size() > 1000) {
        					UpdateResponse response = SolrManager.getInstance().getConcurrentUpdateSolrClient().add(docs);
        					if (response.getStatus() != 0) {
            					log.error("Indexing groups error: " + response.getStatus());
            				}
        					docs.clear();

        				}

    				}

    			}

    		} else {
    			log.debug("No samples to index.");
    		}
			*/

    		log.info("Indexing finished!");

    	} catch (Exception e) {
    		log.error("Error creating index", e);

    	} finally {

    		DataBaseManager.closeConnection();

//        	try {
//            	if (docs.size() > 0) {
//            		SolrManager.getInstance().getConcurrentUpdateSolrClient().add(docs, 300000);
//    			}
//				SolrManager.getInstance().getConcurrentUpdateSolrClient().commit();
//			} catch (SolrServerException | IOException e) {
//				log.error("Error creating index", e);
//			}

        	System.exit(0);
    	}

    }

}
