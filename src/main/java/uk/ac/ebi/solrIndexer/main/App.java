package uk.ac.ebi.solrIndexer.main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.core_model.persistence.dao.hibernate.toplevel.AccessibleDAO;
import uk.ac.ebi.fg.core_model.resources.Resources;
import uk.ac.ebi.solrIndexer.common.Formater;
import uk.ac.ebi.solrIndexer.threads.ThreadGroup;
import uk.ac.ebi.solrIndexer.threads.ThreadGroupByOffset;
import uk.ac.ebi.solrIndexer.threads.ThreadSample;
import uk.ac.ebi.solrIndexer.threads.ThreadSampleByOffset;

@Component
public class App implements ApplicationRunner {
	
	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Value("${threadcount}")
	private int poolThreadCount;
	
	@Value("${samples.fetchStep}")
	private int samplesFetchStep;
	
	@Value("${groups.fetchStep}")
	private int groupsFetchStep;
	
	@Value("${solrIndexer.corePath}")
	private String solrIndexCorePath;
	@Value("${solrIndexer.queueSize}")
	private int solrIndexQueueSize;
	@Value("${solrIndexer.threadCount}")
	private int solrIndexThreadCount;
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("Entering application.");
		long startTime = System.currentTimeMillis();

		ExecutorService threadPool = Executors.newFixedThreadPool(poolThreadCount);
		ConcurrentUpdateSolrClient client = new ConcurrentUpdateSolrClient(solrIndexCorePath, solrIndexQueueSize, solrIndexThreadCount);
		client.setParser(new XMLResponseParser());

		List<Future<Integer>> futures = new ArrayList<Future<Integer>>();
		int callableCount = 0;

		try {

			//Handle Groups
			log.info("Handling Groups");
			
	        int groupCount = DataBaseManager.getGroupCount();
	        
	        log.info("Counted "+groupCount+" groups");
	        
	        for (int i = 0; i < groupCount; i+=groupsFetchStep) {
				futures.add(threadPool.submit(new ThreadGroupByOffset(client, i, groupsFetchStep)));
				/*
				//check and remove any previous futures that have finished
				Iterator<Future<Integer>> iter = futures.iterator();
				while (iter.hasNext()) {
					Future<Integer> future = iter.next();
					if (future.isDone()) {
						callableCount += future.get();
						log.info(""+callableCount+" sucessful callables so far...");
						iter.remove();
					}
				}
				*/
	        }
	        
			//Handle Samples
			
			log.info("Handling Samples");
			//reset counters
			callableCount = 0;			

	        int sampleCount = DataBaseManager.getSampleCount();
	        
	        log.info("Counted "+sampleCount+" samples");
	        
	        for (int i = 0; i < sampleCount; i+=samplesFetchStep) {
				futures.add(threadPool.submit(new ThreadSampleByOffset(client, i, samplesFetchStep)));
				/*
				//check and remove any previous futures that have finished
				Iterator<Future<Integer>> iter = futures.iterator();
				while (iter.hasNext()) {
					Future<Integer> future = iter.next();
					if (future.isDone()) {
						callableCount += future.get();
						log.info(""+callableCount+" sucessful callables so far...");
						iter.remove();
					}
				}
				*/
	        }

	        //wait for all other futures to finish
			for (Future<Integer> future : futures) {
				callableCount += future.get();
				log.info(""+callableCount+" sucessful callables so far...");
			}

			log.info("Generated documents from "+callableCount+" sucessful callables in "+Formater.formatTime(System.currentTimeMillis() - startTime));
			
			//finish handling samples

			log.info("Indexing finished!");

		} catch (Exception e) {
			log.error("Error creating index", e);

		} finally {
			client.close();
			log.info("Running time: " + Formater.formatTime(System.currentTimeMillis() - startTime));

			System.exit(0);
		}
	}

}