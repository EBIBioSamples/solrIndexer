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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.core_model.persistence.dao.hibernate.toplevel.AccessibleDAO;
import uk.ac.ebi.fg.core_model.resources.Resources;
import uk.ac.ebi.solrIndexer.common.Formater;
import uk.ac.ebi.solrIndexer.main.repo.BioSampleGroupRepository;
import uk.ac.ebi.solrIndexer.main.repo.BioSampleRepository;
import uk.ac.ebi.solrIndexer.threads.GroupCallable;
import uk.ac.ebi.solrIndexer.threads.SampleCallable;

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
	
	@Override
	@Transactional
	public void run(ApplicationArguments args) throws Exception {
		log.info("Entering application.");
		long startTime = System.currentTimeMillis();
		if (poolThreadCount > 0) {
			threadPool = Executors.newFixedThreadPool(poolThreadCount);
		}
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
        
        for (int i = 0; i < (groupCount/groupsFetchStep)+1; i++) {
        	Callable<Integer> callable = new GroupCallable(bioSampleGroupRepository.findAll(new PageRequest(i, groupsFetchStep)), client);
			if (poolThreadCount == 0) {
				callable.call();
			} else {
				futures.add(threadPool.submit(callable));
			}
        }

        //wait for all other futures to finish
		for (Future<Integer> future : futures) {
			callableCount += future.get();
			log.info(""+callableCount+" sucessful callables so far...");
		}
		
		client.commit();		
		client.close();

		log.info("Generated documents from "+callableCount+" sucessful callables in "+Formater.formatTime(System.currentTimeMillis() - startTime));
		
		//finish handling samples

		log.info("Indexing finished!");
		
		return;
	}
}