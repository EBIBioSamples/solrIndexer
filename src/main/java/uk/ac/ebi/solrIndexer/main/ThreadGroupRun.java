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

import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.solrIndexer.properties.PropertiesManager;

public class ThreadGroupRun implements Runnable {
	private static Logger log = LoggerFactory.getLogger(ThreadGroupRun.class.getName());

	private List<BioSampleGroup> groupsForThread;

	public ThreadGroupRun (List<BioSampleGroup> groups) {
		this.groupsForThread = groups;
	}

	@Override
	public void run() {
		ConcurrentUpdateSolrClient client = new ConcurrentUpdateSolrClient(PropertiesManager.getSolrCorePath(), 10, Runtime.getRuntime().availableProcessors());
		client.setParser(new XMLResponseParser());

		Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
		try {
			for (BioSampleGroup group : groupsForThread) {
				SolrInputDocument document = SolrManager.generateBioSampleGroupSolrDocument(group);

				if (document != null) {
    				docs.add(document);

    				if (docs.size() > 9999) {
    					UpdateResponse response = client.add(docs);
    					client.commit();
    					if (response.getStatus() != 0) {
    						log.error("Indexing groups error: " + response.getStatus());
    					}
    					docs.clear();
    				}
				}
			}

		} catch (Exception e) {
			log.error("Error generating groups documents.", e);
		} finally {
			try {
				if (docs.size() > 0) {
	        		client.add(docs);
	        		client.commit();
				}

				docs.clear();
				DataBaseManager.closeDataBaseConnection();
				client.close();
			} catch (SolrServerException | IOException e) {
				log.error("Error generating groups documents.", e);
			}
		}
	}

}
