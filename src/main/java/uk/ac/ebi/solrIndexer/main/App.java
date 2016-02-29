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
import uk.ac.ebi.solrIndexer.main.repo.BioSampleGroupRepository;
import uk.ac.ebi.solrIndexer.main.repo.BioSampleRepository;
import uk.ac.ebi.solrIndexer.threads.GroupRepoCallable;

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
	
	
	@Autowired 
	private BioSampleRepository bioSampleRepository;
	@Autowired 
	private BioSampleGroupRepository bioSampleGroupRepository;

	private ExecutorService threadPool = null;

	@Autowired
	private ApplicationContext context;
	
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
			client.setParser(new XMLResponseParser());
	
			List<Future<Integer>> futures = new ArrayList<Future<Integer>>();
			int callableCount = 0;
	
			//DataBaseManager dbm = new DataBaseManager();
	        //int groupCount = dbm.getGroupCount();
			
			int sampleCount = (int) bioSampleRepository.count();
			int groupCount = (int) bioSampleGroupRepository.count();
	
	        log.info("Counted "+groupCount+" groups");
	        log.info("Counted "+groupCount+" samples");
	        
			//Handle Groups
			log.info("Handling Groups");
			
			log.info(bioSampleRepository.getClass().getName());
			
	        for (int i = 0; i < groupCount; i+= groupsFetchStep) {
	        	//Callable<Integer> callable;
	        	//can't pass the repository around as then its not trasactional
	        	//callable = new GroupPageCallable(i, groupsFetchStep, bioSampleGroupRepository, client);
	        	
	        	//cant pass a page around as then hibernate has concurrent modificaiton exceptions
	        	//Page<BioSampleGroup> page = bioSampleGroupRepository.findAll(new PageRequest(i/groupsFetchStep, groupsFetchStep));
	        	//callable = new GroupCallable(page, client);
	        	
	        	//can't autowire the repository as then its a signleton
	        	//callable = new GroupRepoCallable(i, groupsFetchStep, client);
	        	GroupRepoCallable callable = context.getBean(GroupRepoCallable.class);
	        	callable.setClient(client);
	        	callable.setPageStart(i);
	        	callable.setPageSize(groupsFetchStep);
	        	
	        	
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
				threadPool.awaitTermination(1, TimeUnit.DAYS);
			}
			
			log.info("closing solr client");
			//finish the solr client
			client.commit();		
			client.close();
	
			log.info("Generated documents from "+callableCount+" sucessful callables in "+Formater.formatTime(System.currentTimeMillis() - startTime));
			
			//finish handling samples
	
			log.info("Indexing finished!");
		} finally {
			if (threadPool != null && !threadPool.isShutdown()) {
				threadPool.shutdownNow();
			}
		}
		
		return;
	}
}