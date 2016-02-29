package uk.ac.ebi.solrIndexer.threads;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.solrIndexer.main.SolrManager;

public class GroupCallable implements Callable<Integer> {
	private Logger log = LoggerFactory.getLogger(this.getClass());

	private Iterable<BioSampleGroup> groupsForThread;
	private ConcurrentUpdateSolrClient client;

	public GroupCallable (Iterable<BioSampleGroup> groups, ConcurrentUpdateSolrClient client) {
		this.groupsForThread = groups;
		this.client = client;
	}

	@Override
	public Integer call() throws Exception {
		Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
		SolrManager solrManager = new SolrManager();

		for (BioSampleGroup group : groupsForThread) {
			log.info("Creating solr document for group "+group.getAcc());
			client.add(solrManager.generateBioSampleGroupSolrDocument(group));
		}

		return 1;
	}

}
