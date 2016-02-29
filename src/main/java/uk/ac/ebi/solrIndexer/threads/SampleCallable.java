package uk.ac.ebi.solrIndexer.threads;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

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
		Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
		SolrManager solrManager = new SolrManager();
		
		for (BioSample sample : samplesForThread) {
			log.info("Creating solr document for group "+sample.getAcc());
			client.add(solrManager.generateBioSampleSolrDocument(sample));
		}
	
		return 1;
	}

}
