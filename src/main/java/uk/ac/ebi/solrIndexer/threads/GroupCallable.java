package uk.ac.ebi.solrIndexer.threads;

import java.util.Optional;
import java.util.concurrent.Callable;

import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.solrIndexer.main.SolrManager;

@Component
public class GroupCallable implements Callable<Integer> {
	private Logger log = LoggerFactory.getLogger(this.getClass());

	protected Iterable<BioSampleGroup> groups;
	protected ConcurrentUpdateSolrClient client;
	
	@Autowired
	private SolrManager solrManager;

	@Value("${solrIndexer.commitWithin:60000}")
	private int commitWithin;

	public GroupCallable() {
		
	}
	
	public GroupCallable (Iterable<BioSampleGroup> groups, ConcurrentUpdateSolrClient client) {
		this.groups = groups;
		this.client = client;
	}

	@Override
	public Integer call() throws Exception {
		for (BioSampleGroup group : groups) {
			log.trace("Creating solr document for group "+group.getAcc());
			Optional<SolrInputDocument> doc = solrManager.generateBioSampleGroupSolrDocument(group);
			if (doc.isPresent()) {
				client.add(doc.get(), commitWithin);
			}
			log.trace("Finished solr document for group "+group.getAcc());
		}

		return 1;
	}

}
