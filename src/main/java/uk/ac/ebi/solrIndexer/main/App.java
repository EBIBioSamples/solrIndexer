package uk.ac.ebi.solrIndexer.main;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;

public class App {
	private static Logger log = LoggerFactory.getLogger(App.class.getName());
	
	public static void main( String[] args ) {
		log.info("Entering application.");

		ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		try {
			int offset;

			/* -- Handle Groups -- */
			log.info("Handling Groups");
			offset = 0;

			List<BioSampleGroup> groups;
			while ((groups = DataBaseManager.getAllIterableGroups(offset, 10000)).size() > 0) {

				final List<BioSampleGroup> groupsForThread = groups;
				threadPool.submit(new ThreadGroupRun(groupsForThread));

				offset += groups.size();
			}
			boolean success = threadPool.awaitTermination(25, TimeUnit.MINUTES);

			log.info("Group documents generated with success: " + success);
			/* -------------------- */

			/* -- Handle Samples -- *
			log.info("Handling Samples");
			offset = 0;

			List<BioSample> samples;
			while ( (samples = DataBaseManager.getAllIterableSamples(offset, 10000)).size() > 0) {
				
				for (BioSample sample : samples) {
					SolrInputDocument document = SolrManager.generateBioSampleSolrDocument(sample);

					if (document != null) {
	    				docs.add(document);

	    				if (docs.size() > 999) {
	    					UpdateResponse response = client.add(docs, 30000);
	    					if (response.getStatus() != 0) {
	    						log.error("Indexing samples error: " + response.getStatus());
	    					}
	    					docs.clear();
	    				}
					}
				}

				offset += samples.size();
			}
			log.info("Sample documents generated.");
			/* -------------------- */

			log.info("Indexing finished!");

		} catch (Exception e) {
			log.error("Error creating index", e);

		} finally {
	    	System.exit(0);
		}

	}

}