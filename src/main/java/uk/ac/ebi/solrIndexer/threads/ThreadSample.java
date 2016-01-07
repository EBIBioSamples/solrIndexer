package uk.ac.ebi.solrIndexer.threads;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.solrIndexer.common.PropertiesManager;
import uk.ac.ebi.solrIndexer.main.SolrManager;

public class ThreadSample implements Callable<Integer> {
	private static Logger log = LoggerFactory.getLogger(ThreadSample.class.getName());

	private int status = 1;
	private List<BioSample> samplesForThread;

	public ThreadSample (List<BioSample> samples) {
		this.samplesForThread = samples;
	}
	
	@Override
	public Integer call() throws Exception {
		ConcurrentUpdateSolrClient client = new ConcurrentUpdateSolrClient(PropertiesManager.getSolrCorePath(), 10, 4);
		client.setParser(new XMLResponseParser());

		Collection<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
		try {
			for (BioSample sample : samplesForThread) {
				SolrInputDocument document = SolrManager.generateBioSampleSolrDocument(sample);
	
				if (document != null) {
					docs.add(document);
	
					if (docs.size() > 9999) {
						UpdateResponse response = client.add(docs);
						client.commit();
						if (response.getStatus() != 0) {
							log.error("Indexing samples error: " + response.getStatus());
						}
						docs.clear();
					}
				}
			}
	
		} catch (Exception e) {
			status = 0;
			log.error("Error generating samples documents.", e);
		} finally {
			try {
				if (docs.size() > 0) {
	        		client.add(docs);
	        		client.commit();
				}

				docs.clear();
				client.close();
			} catch (SolrServerException | IOException e) {
				log.error("Error generating samples documents.", e);
			}
		}

		return status;
	}

}
