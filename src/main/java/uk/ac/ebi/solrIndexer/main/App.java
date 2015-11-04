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

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.sampletab.loader.Loader;

public class App {
	private static Logger log = LoggerFactory.getLogger(App.class.getName());

    public static void main( String[] args ) {
    	log.info("Entering application.");
    	
    	DataBaseConnection dbi = null;
    	Collection<SolrInputDocument> docs = null;

    	try {
    		/* --- Populate the in-memory DB --- 
    		MSI msi1 = null, msi2 = null;
        	try {
        		// SampleTab File 1
        		if(msi1 == null) {
        			msi1 = loadSampleTab (args [0]);
        			if(msi1.getSamples().size() + msi1.getSampleGroups().size() > 0) {
        				new Persister ().persist (msi1);
        			}
        		}
        		// SampleTab File 2
        		if(msi2 == null) {
        			msi2 = loadSampleTab (args [1]);
        			if(msi2.getSamples().size() + msi2.getSampleGroups().size() > 0) {
        				new Persister ().persist (msi2);
        			}
        		}
        	} catch (RuntimeException | ParseException e) {
    			msi1 = null;
    			msi2 = null;
        		e.printStackTrace();
        	}
        	/* --- ------------------------- --- */

    		dbi = DataBaseConnection.getInstance();
    		docs = new ArrayList<SolrInputDocument>();

    		List<BioSampleGroup> groups = BioSDEntities.fetchGroups();
    		if (!groups.isEmpty()) {
    			log.info("Generating Solr group documents");
        		for (BioSampleGroup bsg : groups) {
        			docs.add(SolrIndexer.generateBioSampleGroupSolrDocument(bsg));
        			if (docs.size() > 1000) {
        				UpdateResponse response = SolrIndexer.getInstance().getConcurrentUpdateSolrClient().add(docs);
        				if (response.getStatus() != 0) {
        					log.error("Indexing groups error: " + response.getStatus());
        				}
        				docs.clear();
        			}
        		}
    		}

    		List<BioSample> samples = BioSDEntities.fetchSamples();
    		if (!samples.isEmpty()) {
    			log.info("Generating Solr sample documents");
        		for (BioSample bs : samples) {
        			docs.add(SolrIndexer.generateBioSampleSolrDocument(bs));
        			if (docs.size() > 1000) {
        				UpdateResponse response = SolrIndexer.getInstance().getConcurrentUpdateSolrClient().add(docs);
        				if (response.getStatus() != 0) {
        					log.error("Indexing groups error: " + response.getStatus());
        				}
        				docs.clear();
        			}
        		}
    		}

    	} catch (Exception e) {
    		log.error("Error creating index", e);
    		e.printStackTrace();
    	} finally {
        	if (dbi.getEntityManager()!= null && dbi.getEntityManager().isOpen()) {
        		dbi.getEntityManager().close();
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

	private static MSI loadSampleTab (String path) throws ParseException {
    	Loader loader = new Loader();
    	MSI msi = loader.fromSampleData (path);
		return msi;
    }

}
