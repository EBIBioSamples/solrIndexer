package uk.ac.ebi.solrIndexer.main;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.solrIndexer.common.Formater;
import uk.ac.ebi.solrIndexer.common.PropertiesManager;
import uk.ac.ebi.solrIndexer.threads.ThreadSample;

public class App {
	private static Logger log = LoggerFactory.getLogger(App.class.getName());
	
	public static void main( String[] args ) {
		log.info("Entering application.");
		long startTime = System.currentTimeMillis();

		ExecutorService threadPool = Executors.newFixedThreadPool(16);

		Set<Future<Integer>> set = new HashSet<Future<Integer>>();

		try {
			int offset, sum;

			/* -- Handle Groups -- *
			log.info("Handling Groups");
			offset = 0;

			List<BioSampleGroup> groups;
			while ((groups = DataBaseManager.getAllIterableGroups(offset, 10000)).size() > 0) {

				final List<BioSampleGroup> groupsForThread = groups;
				Future<Integer> future = threadPool.submit(new ThreadGroup(groupsForThread, client));
				set.add(future);

				offset += groups.size();
			}

			sum = 0;
			for (Future<Integer> future : set) {
				sum += future.get();
			}

			log.info("Group documents generated." + sum + " threads finished successfully.");
			/* -------------------- */

			/* -- Handle Samples -- */
			log.info("Handling Samples");
			offset = 0;

			int i = 0;
			List<BioSample> samples;
			while ( (samples = DataBaseManager.getAllIterableSamples(offset, PropertiesManager.getSamplesFetchStep())).size() > 0) {

				final List<BioSample> samplesForThread = samples;
				Future<Integer> future = threadPool.submit(new ThreadSample(samplesForThread));
				set.add(future);

				offset += samples.size();
				
				i++;
				if (i%8 == 0 && i != 0)
					future.get();//Wait...
			}

			sum = 0;
			for (Future<Integer> future : set) {
				sum += future.get();
			}

			log.info("Sample documents generated." + sum + " threads finished successfully.");
			/* -------------------- */

			log.info("Indexing finished!");

		} catch (Exception e) {
			log.error("Error creating index", e);

		} finally {
			long duration = (System.currentTimeMillis() - startTime);
			log.info(Formater.formatTime(duration));

			System.exit(0);
		}

	}

}