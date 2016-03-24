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

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.solrIndexer.main.SolrManager;

@Component
public class SampleCallable implements Callable<Integer> {
	private Logger log = LoggerFactory.getLogger(this.getClass());

	protected Iterable<BioSample> samples;
	protected ConcurrentUpdateSolrClient client;
	
	@Autowired
	private SolrManager solrManager;

	@Value("${solrIndexer.commitWithin:60000}")
	private int commitWithin;

	public SampleCallable() {
		
	}
	
	public SampleCallable (Iterable<BioSample> samples, ConcurrentUpdateSolrClient client) {
		this.samples = samples;
		this.client = client;
	}

	@Override
	public Integer call() throws Exception {	
		for (BioSample sample : samples) {
			if (sample == null) continue;
			Optional<SolrInputDocument> doc = solrManager.generateBioSampleSolrDocument(sample);
			if (doc.isPresent()) {
				client.add(doc.get(), commitWithin);
			}
		}
	
		return 1;
	}

}
