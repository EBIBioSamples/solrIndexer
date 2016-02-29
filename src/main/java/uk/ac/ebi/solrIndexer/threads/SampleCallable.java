package uk.ac.ebi.solrIndexer.threads;

import java.util.concurrent.Callable;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.solrIndexer.main.SolrManager;

public class SampleCallable implements Callable<Integer> {
	private Logger log = LoggerFactory.getLogger(this.getClass());

	private final Iterable<BioSample> samplesForThread;
	private final ConcurrentUpdateSolrClient client;

	public SampleCallable (Iterable<BioSample> samples, ConcurrentUpdateSolrClient client) {
		this.samplesForThread = samples;
		this.client = client;
	}

	@Override
	public Integer call() throws Exception {
		SolrManager solrManager = new SolrManager();		
		for (BioSample sample : samplesForThread) {
			log.trace("Creating solr document for sample "+sample.getAcc());
			SolrInputDocument doc = solrManager.generateBioSampleSolrDocument(sample);
			if (doc != null) {
				client.add(doc);
			}
			log.trace("Finished solr document for sample "+sample.getAcc());
		}
	
		return 1;
	}

}
