package uk.ac.ebi.solrIndexer.main;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ebi.solrIndexer.common.Formater;
import uk.ac.ebi.solrIndexer.threads.GroupRepoCallable;
import uk.ac.ebi.solrIndexer.threads.SampleRepoCallable;

@Component
public class App implements ApplicationRunner {
	
	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Value("${threadcount:0}")
	private int poolThreadCount;
	
	@Value("${samples.fetchStep:1000}")
	private int samplesFetchStep;
	
	@Value("${groups.fetchStep:1000}")
	private int groupsFetchStep;
	
	@Value("${solrIndexer.corePath}")
	private String solrIndexCorePath;
	@Value("${solrIndexer.queueSize:1000}")
	private int solrIndexQueueSize;
	@Value("${solrIndexer.threadCount:4}")
	private int solrIndexThreadCount;
	
	private ExecutorService threadPool = null;

	@Autowired
	private ApplicationContext context;
	
	@Autowired
	private JDBCDAO jdbcdao;
	
	@Override
	@Transactional
	public void run(ApplicationArguments args) throws Exception {
		log.info("Entering application.");
		long startTime = System.currentTimeMillis();
		if (poolThreadCount > 0) {
			threadPool = Executors.newFixedThreadPool(poolThreadCount);
		}
		try{
			ConcurrentUpdateSolrClient client = new ConcurrentUpdateSolrClient(solrIndexCorePath, solrIndexQueueSize, solrIndexThreadCount);
			//client.setParser(new XMLResponseParser());
			log.warn("DELETING EXISTING SOLR INDEX!!!");
			client.deleteByQuery( "*:*" );// CAUTION: deletes everything!
	
			List<Future<Integer>> futures = new ArrayList<Future<Integer>>();
			int callableCount = 0;
	
			//DataBaseManager dbm = new DataBaseManager();
	        //int groupCount = dbm.getGroupCount();
			
			log.info("Getting group accessions");
			List<String> groupAccs = jdbcdao.getPublicGroups();
	        log.info("got "+groupAccs.size()+" groups");
	        
			//Handle Groups
			log.info("Handling Groups");
			
	        for (int i = 0; i < groupAccs.size(); i+= groupsFetchStep) {	        	
	        	//have to create multiple beans via context so they all have their own dao object
	        	//this is apparently bad Inversion Of Control but I can't see a better way to do it
	        	GroupRepoCallable callable = context.getBean("groupRepoCallable", GroupRepoCallable.class);
	        	
	        	callable.setClient(client);
	        	callable.setAccessions(groupAccs.subList(i, Math.min(i+groupsFetchStep, groupAccs.size())));
	        	
	        	
				if (poolThreadCount == 0) {
					callable.call();
				} else {
					futures.add(threadPool.submit(callable));
				}
	        }

	        log.info("Getting sample accessions");
			List<String> sampleAccs = jdbcdao.getPublicSamples();
	        log.info("Counted "+sampleAccs.size()+" samples");
	        
			//Handle samples
			log.info("Handling samples");
			
	        for (int i = 0; i < sampleAccs.size(); i+= samplesFetchStep) {
	        	SampleRepoCallable callable = context.getBean("sampleRepoCallable", SampleRepoCallable.class);
	        	
	        	callable.setClient(client);	
	        	callable.setAccessions(sampleAccs.subList(i, Math.min(i+samplesFetchStep, sampleAccs.size())));
	        	
				if (poolThreadCount == 0) {
					callable.call();
				} else {
					futures.add(threadPool.submit(callable));
				}
	        }
	
	        //wait for all other futures to finish
	        log.info("Waiting for futures...");
			for (Future<Integer> future : futures) {
				callableCount += future.get();
				log.trace(""+callableCount+" sucessful callables so far...");
			}
			
			//close down thread pool
			if (threadPool != null) {
		        log.info("Shutting down thread pool");
		        threadPool.shutdown();
				threadPool.awaitTermination(1, TimeUnit.DAYS);
			}
			
			log.info("Closing solr client");
			//finish the solr client
			client.commit();		
			client.close();
	
			log.info("Generated documents from "+callableCount+" sucessful callables in "+Formater.formatTime(System.currentTimeMillis() - startTime));
			
			//finish handling samples
	
			log.info("Indexing finished!");
		} finally {
			if (threadPool != null && !threadPool.isShutdown()) {
		        log.info("Shutting down thread pool");
				threadPool.shutdownNow();
			}
		}
		
		return;
	}
}