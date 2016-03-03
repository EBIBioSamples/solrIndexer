package uk.ac.ebi.solrIndexer.main;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.solr.client.solrj.impl.ConcurrentUpdateSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ebi.fg.biosd.annotator.persistence.AnnotatorAccessor;
import uk.ac.ebi.fg.core_model.resources.Resources;
import uk.ac.ebi.solrIndexer.common.Formater;
import uk.ac.ebi.solrIndexer.threads.GroupRepoCallable;
import uk.ac.ebi.solrIndexer.threads.SampleRepoCallable;

@Component 
@PropertySource("solrIndexer.properties")
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

	@Value("${onto.mapping.annotator:false}")
	private boolean useAnnotator;
	
	private ExecutorService threadPool = null;
	private List<Future<Integer>> futures = new ArrayList<Future<Integer>>();
	private int callableCount = 0;

	@Autowired
	private ApplicationContext context;
	
	@Autowired
	private BioSDDAO jdbcdao;
	
	@Autowired
	private SolrManager solrManager;

	private ConcurrentUpdateSolrClient client = null;
	private List<String> groupAccs;
	private List<String> sampleAccs; 
	
	private boolean doGroups = true;
	private boolean doSamples = true;
	private int offsetCount = 0;
	private int offsetTotal = -1;
	
	@Override
	@Transactional
	public void run(ApplicationArguments args) throws Exception {
		log.info("Entering application.");
		long startTime = System.currentTimeMillis();
		

        //process arguments
		if (args.containsOption("offsetcount")) {
			//subtract one so we use 1 to Total externally and 0 to (Total-1) internally
			//better human readable and LSF compatability
			offsetCount = Integer.parseInt(args.getOptionValues("offsetcount").get(0))-1;
		}
		if (args.containsOption("offsettotal")) {
			offsetTotal = Integer.parseInt(args.getOptionValues("offsettotal").get(0));
		}
		doGroups = !args.containsOption("notgroups");
		doSamples = !args.containsOption("notsamples");
		solrManager.setIncludeXML(args.containsOption("includexml"));
        

		if (doGroups) {
			if (offsetTotal > 0) {
				int count = jdbcdao.getGroupCount();
				int offsetSize = count/offsetTotal;
				int start = offsetSize*offsetCount;
				log.info("Getting group accessions for chunk "+offsetCount+" of "+offsetTotal);
				groupAccs = jdbcdao.getGroupAccessions(start, offsetSize);
		        log.info("got "+groupAccs.size()+" groups");
			} else {
				log.info("Getting group accessions");
				groupAccs = jdbcdao.getGroupAccessions();
		        log.info("got "+groupAccs.size()+" groups");
			}
		}
		if (doSamples) {
			if (offsetTotal > 0) {
				int count = jdbcdao.getSampleCount();
				int offsetSize = count/offsetTotal;
				int start = offsetSize*offsetCount;
				log.info("Getting sample accessions for chunk "+offsetCount+" of "+offsetTotal);
				sampleAccs =jdbcdao.getSampleAccessions(start, offsetSize);
		        log.info("got "+sampleAccs.size()+" groups");
			} else {
		        log.info("Getting sample accessions");
				sampleAccs = jdbcdao.getSampleAccessions();
		        log.info("Counted "+sampleAccs.size()+" samples");
			}
		}
        
		try{
			//create solr index
			client = new ConcurrentUpdateSolrClient(solrIndexCorePath, solrIndexQueueSize, solrIndexThreadCount);
			//maybe we want this, maybe not?
			//client.setParser(new XMLResponseParser());
			log.warn("DELETING EXISTING SOLR INDEX!!!");
			client.deleteByQuery( "*:*" );// CAUTION: deletes everything!

			//setup annotator, if using
			AnnotatorAccessor annotator = null;
			try {
				if (useAnnotator) {
					annotator = new AnnotatorAccessor(Resources.getInstance().getEntityManagerFactory().createEntityManager());
					//set the solr manager to use the annotator
					//because spring autowires singletons,
					//this is the same solrmanager as the one autowired in the callables
					solrManager.setAnnotator(annotator);
				}
				
		        //create the thread stuff if required
				try {
					if (poolThreadCount > 0) {
						threadPool = Executors.newFixedThreadPool(poolThreadCount);
					}	
				
					//process things
					runGroups(groupAccs);
					runSamples(sampleAccs);				
			
			        //wait for all other futures to finish
			        log.info("Waiting for futures...");
					for (Future<Integer> future : futures) {
						callableCount += future.get();
						log.trace(""+callableCount+" sucessful callables so far, "+futures.size()+" remaining");
						//after each finished callable make the solr client commit
						//populates the index as we go, and doing them all here reduces collision risk
						//if collisions do occur, increase samples.fetchStep and groups.fetchStep
						//client.commit();
						//removing this in favour of commitwithin parameter on add
					}
					
					//close down thread pool
					if (threadPool != null) {
				        log.info("Shutting down thread pool");
				        threadPool.shutdown();
				        //one day is a lot, but better safe than sorry!
						threadPool.awaitTermination(1, TimeUnit.DAYS);
					}		
				} finally {
					//handle closing of thread pool in case of error
					if (threadPool != null && !threadPool.isShutdown()) {
				        log.info("Shutting down thread pool");
				        //allow a second to cleanly terminate before forcing
				        threadPool.shutdown();
						threadPool.awaitTermination(1, TimeUnit.SECONDS);
						threadPool.shutdownNow();
					}
				}
			} finally {
				//handle closing of annotator entity manager
				if (annotator != null) {
					annotator.close();
				}
			}
				
		} finally {

			//finish the solr client
			log.info("Closing solr client");
			client.commit();		
			client.blockUntilFinished();
			client.close();
		}
		
		log.info("Generated documents from "+callableCount+" sucessful callables in "+Formater.formatTime(System.currentTimeMillis() - startTime));
		log.info("Indexing finished!");
		return;
	}
	
	private void runGroups(List<String> groupAccs) throws Exception {
		//Handle Groups
		log.info("Handling Groups");		
        for (int i = 0; i < groupAccs.size(); i += groupsFetchStep) {	        	
        	//have to create multiple beans via context so they all have their own dao object
        	//this is apparently bad Inversion Of Control but I can't see a better way to do it
        	GroupRepoCallable callable = context.getBean(GroupRepoCallable.class);
        	
        	callable.setClient(client);
        	callable.setAccessions(groupAccs.subList(i, Math.min(i+groupsFetchStep, groupAccs.size())));        	
        	
			if (poolThreadCount == 0) {
				callable.call();
			} else {
				futures.add(threadPool.submit(callable));
			}
        }        
	}
	
	private void runSamples(List<String> sampleAccs) throws Exception {
		//Handle samples
		log.info("Handling samples");
        for (int i = 0; i < sampleAccs.size(); i += samplesFetchStep) {
        	//have to create multiple beans via context so they all have their own dao object
        	//this is apparently bad Inversion Of Control but I can't see a better way to do it
        	SampleRepoCallable callable = context.getBean(SampleRepoCallable.class);
        	
        	callable.setClient(client);	
        	callable.setAccessions(sampleAccs.subList(i, Math.min(i+samplesFetchStep, sampleAccs.size())));
        	
			if (poolThreadCount == 0) {
				callable.call();
			} else {
				futures.add(threadPool.submit(callable));
			}
        }
	}
}
