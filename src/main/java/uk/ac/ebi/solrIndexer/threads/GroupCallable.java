package uk.ac.ebi.solrIndexer.threads;

import java.util.Optional;
import java.util.concurrent.Callable;

import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.solrIndexer.main.SolrManager;

@Component
//this makes sure that we have a different instance wherever it is used
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GroupCallable implements Callable<Integer> {
	private Logger log = LoggerFactory.getLogger(this.getClass());

	protected Iterable<BioSampleGroup> groups;
	protected ConcurrentUpdateSolrClient client;
	protected ConcurrentUpdateSolrClient mergedClient;
	
	@Autowired
	private SolrManager solrManager;

	@Value("${solrIndexer.commitWithin:60000}")
	private int commitWithin;

	public GroupCallable() {
		
	}
	
	public GroupCallable (Iterable<BioSampleGroup> groups, ConcurrentUpdateSolrClient client, ConcurrentUpdateSolrClient mergedClient) {
		this.groups = groups;
		this.client = client;
		this.mergedClient = mergedClient;
	}

	@Override
	public Integer call() throws Exception {
		int count = 0;
		for (BioSampleGroup group : groups) {
			if (group == null) continue;
			Optional<SolrInputDocument> doc = solrManager.generateBioSampleGroupSolrDocument(group);
			if (doc.isPresent()) {
				client.add(doc.get(), commitWithin);
				mergedClient.add(doc.get(), commitWithin);
				count += 1;
			}
		}

		return count;
	}

}
