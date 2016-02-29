package uk.ac.ebi.solrIndexer.threads;

import java.util.concurrent.Callable;

import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
		SolrManager solrManager = new SolrManager();
		for (BioSampleGroup group : groupsForThread) {
			log.trace("Creating solr document for group "+group.getAcc());
			SolrInputDocument doc = solrManager.generateBioSampleGroupSolrDocument(group);
			if (doc != null) {
				client.add(doc);
			}
			log.trace("Finished solr document for group "+group.getAcc());
		}

		return 1;
	}

}
