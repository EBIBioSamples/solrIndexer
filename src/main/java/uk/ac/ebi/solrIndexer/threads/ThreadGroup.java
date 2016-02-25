package uk.ac.ebi.solrIndexer.threads;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.solrIndexer.main.SolrManager;

public class ThreadGroup implements Callable<Integer> {
	private static Logger log = LoggerFactory.getLogger(ThreadGroup.class.getName());

	private int status = 1;
	private List<BioSampleGroup> groupsForThread;
	private SolrClient client;
	private int offset;

	public ThreadGroup (List<BioSampleGroup> groups, SolrClient client, int offset) {
		this.groupsForThread = groups;
		this.client = client;
		this.offset = offset;
	}

	@Override
	public Integer call() throws Exception {
		Collection<SolrInputDocument> docs = new ArrayList<>();
		//DataBaseConnection connection = new DataBaseConnection();

		try {
			log.info("Generating group documents for " + groupsForThread.size() + " groups, from position " + offset + "...");
			for (BioSampleGroup group : groupsForThread) {
				SolrInputDocument document = SolrManager.generateBioSampleGroupSolrDocument(group);

				if (document != null) {
    				docs.add(document);

    				if (docs.size() > 9999) {
						log.info("Obtained " + docs.size() + " group documents, writing to index...");
    					UpdateResponse response = client.add(docs);
    					client.commit();
    					if (response.getStatus() != 0) {
    						log.error("Indexing groups error: " + response.getStatus());
    					}
    					docs.clear();
						log.info("Documents written OK");
    				}
				}
			}

		} catch (Exception e) {
			status = 0;
			log.error("Error generating groups documents.", e);
		} finally {
			try {
				if (docs.size() > 0) {
					client.add(docs);
					client.commit();
				}

				docs.clear();
				//connection.closeDataBaseConnection();

			} catch (SolrServerException | IOException e) {
				log.error("Error generating groups documents.", e);
			}
		}

		return status;
	}

}
