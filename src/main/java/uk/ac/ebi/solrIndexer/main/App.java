package uk.ac.ebi.solrIndexer.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

	@Value("${solrIndexer.groups.corePath}")
	private String solrIndexGroupsCorePath;
	@Value("${solrIndexer.samples.corePath}")
	private String solrIndexSamplesCorePath;
	@Value("${solrIndexer.merged.corePath}")
	private String solrIndexMergedCorePath;

	@Value("${solrIndexer.queueSize:1000}")
	private int solrIndexQueueSize;
	@Value("${solrIndexer.threadCount:4}")
	private int solrIndexThreadCount;

	@Value("${onto.mapping.annotator:false}")
	private boolean useAnnotator;

	private ExecutorService threadPool = null;
	private List<Future<Integer>> futures = new ArrayList<>();
	private int callableCount = 0;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private BioSDDAO jdbcdao;

	@Autowired
	private SolrManager solrManager;

	private ConcurrentUpdateSolrClient groupsClient = null;
	private ConcurrentUpdateSolrClient samplesClient = null;
	private ConcurrentUpdateSolrClient mergedClient = null;
	private List<String> groupAccs = new ArrayList<>();
	private List<String> sampleAccs = new ArrayList<>();;

	private boolean cleanup = false;
	private boolean doGroups = true;
	private boolean doSamples = true;
	private int offsetCount = 0;
	private int offsetTotal = -1;

	@Override
	@Transactional
	public void run(ApplicationArguments args) throws Exception {
		log.info("Entering application.");
		long startTime = System.currentTimeMillis();

		// process arguments
		if (args.containsOption("offsetcount")) {
			// subtract one so we use 1 to Total externally and 0 to (Total-1)
			// internally
			// better human readable and LSF compatability
			offsetCount = Integer.parseInt(args.getOptionValues("offsetcount").get(0)) - 1;
		}
		if (args.containsOption("offsettotal")) {
			offsetTotal = Integer.parseInt(args.getOptionValues("offsettotal").get(0));
		}
		// wipes the existing index
		// will only apply if offsetCount==0
		cleanup = args.containsOption("cleanup");
		doGroups = !args.containsOption("notgroups");
		doSamples = !args.containsOption("notsamples");
		solrManager.setIncludeXML(args.containsOption("includexml"));
		if (args.containsOption("sourcefile")) {
			//if -- is a filename then read stdin
			handleFilenames(args.getOptionValues("sourcefile"));
		}

		// Merged core client initialization
		mergedClient = new ConcurrentUpdateSolrClient(solrIndexMergedCorePath, solrIndexQueueSize, solrIndexThreadCount);

		// only bother getting accessions from db if we will actually use them
		if (doGroups) {
			groupsClient = new ConcurrentUpdateSolrClient(solrIndexGroupsCorePath, solrIndexQueueSize, solrIndexThreadCount);
			// don't get from db if we were given a file with them
			if (groupAccs.size() == 0) {
				if (offsetTotal > 0) {
					int count = jdbcdao.getGroupCount();
					int offsetSize = count / offsetTotal;
					int start = offsetSize * offsetCount;
					log.info("Getting group accessions for chunk " + offsetCount + " of " + offsetTotal);
					groupAccs = jdbcdao.getGroupAccessions(start, offsetSize);
					log.info("got " + groupAccs.size() + " groups");
				} else {
					log.info("Getting group accessions");
					groupAccs = jdbcdao.getGroupAccessions();
					log.info("got " + groupAccs.size() + " groups");
				}
			}
		}
		// only bother getting accessions from db if we will actually use them
		if (doSamples) {
			samplesClient = new ConcurrentUpdateSolrClient(solrIndexSamplesCorePath, solrIndexQueueSize, solrIndexThreadCount);
			// don't get from db if we were given a file with them
			if (sampleAccs.size() == 0) {
				if (offsetTotal > 0) {
					int count = jdbcdao.getSampleCount();
					int offsetSize = count / offsetTotal;
					int start = offsetSize * offsetCount;
					log.info("Getting sample accessions for chunk " + offsetCount + " of " + offsetTotal);
					sampleAccs = jdbcdao.getSampleAccessions(start, offsetSize);
					log.info("got " + sampleAccs.size() + " samples");
				} else {
					log.info("Getting sample accessions");
					sampleAccs = jdbcdao.getSampleAccessions();
					log.info("Counted " + sampleAccs.size() + " samples");
				}
			}
		}

		try {
			// create solr index
			// maybe we want this, maybe not?
			// client.setParser(new XMLResponseParser());
			if (cleanup && offsetCount == 0) {
				log.warn("DELETING EXISTING SOLR INDEX!!!");
				// CAUTION: deletes everything!
				if (groupsClient != null) {
					groupsClient.deleteByQuery("*:*");
				}
				if (samplesClient != null) {
					samplesClient.deleteByQuery("*:*");
				}
				if (mergedClient != null) {
					mergedClient.deleteByQuery("*:*");
				}
			}

			// setup annotator, if using
			AnnotatorAccessor annotator = null;
			try {
				if (useAnnotator) {
					annotator = new AnnotatorAccessor(
							Resources.getInstance().getEntityManagerFactory().createEntityManager());
					// set the solr manager to use the annotator
					// because spring autowires singletons,
					// this is the same solrmanager as the one autowired in the
					// callables
					solrManager.setAnnotator(annotator);
				}

				// create the thread stuff if required
				try {
					if (poolThreadCount > 0) {
						threadPool = Executors.newFixedThreadPool(poolThreadCount);
					}

					// process things
					if (mergedClient != null) {
						if (doGroups && groupsClient != null) {
							runGroups(groupAccs);
						}
						if (doSamples && samplesClient != null) {
							runSamples(sampleAccs);
						}
					}

					// wait for all other futures to finish
					log.info("Waiting for futures...");
					for (Future<Integer> future : futures) {
						callableCount += future.get();
						log.trace("" + callableCount + " sucessful callables so far, " + futures.size() + " remaining");
						// after each finished callable make the solr client
						// commit
						// populates the index as we go, and doing them all here
						// reduces collision risk
						// if collisions do occur, increase samples.fetchStep
						// and groups.fetchStep
						// client.commit();
						// removing this in favour of commit within parameter on
						// add
					}

					// close down thread pool
					if (threadPool != null) {
						log.info("Shutting down thread pool");
						threadPool.shutdown();
						// one day is a lot, but better safe than sorry!
						threadPool.awaitTermination(1, TimeUnit.DAYS);
					}
				} finally {
					// handle closing of thread pool in case of error
					if (threadPool != null && !threadPool.isShutdown()) {
						log.info("Shutting down thread pool");
						// allow a second to cleanly terminate before forcing
						threadPool.shutdown();
						threadPool.awaitTermination(1, TimeUnit.SECONDS);
						threadPool.shutdownNow();
					}
				}
			} finally {
				// handle closing of annotator entity manager
				if (annotator != null) {
					annotator.close();
				}
			}

		} finally {

			// finish the solr clients
			log.info("Closing solr clients");
			if (groupsClient != null) {
				groupsClient.commit();
				groupsClient.blockUntilFinished();
				groupsClient.close();
			}

			if (samplesClient != null) {
				samplesClient.commit();
				samplesClient.blockUntilFinished();
				samplesClient.close();
			}

			if (mergedClient != null) {
				mergedClient.commit();
				mergedClient.blockUntilFinished();
				mergedClient.close();
			}
		}

		log.info("Generated documents from " + callableCount + " sucessful callables in "
				+ Formater.formatTime(System.currentTimeMillis() - startTime));
		log.info("Indexing finished!");
		return;
	}

	private void runGroups(List<String> groupAccs) throws Exception {
		if (doGroups) {
			// Handle Groups
			log.info("Handling Groups");
			for (int i = 0; i < groupAccs.size(); i += groupsFetchStep) {
				List<String> theseGroupAccs = groupAccs.subList(i, Math.min(i + groupsFetchStep, groupAccs.size()));
				// have to create multiple beans via context so they all have
				// their own dao object
				// this is apparently bad Inversion Of Control but I can't see a
				// better way to do it
				GroupRepoCallable callable = context.getBean(GroupRepoCallable.class, groupsClient, mergedClient, theseGroupAccs);

				if (poolThreadCount == 0) {
					callable.call();
				} else {
					futures.add(threadPool.submit(callable));
				}
			}
		}
	}

	private void runSamples(List<String> sampleAccs) throws Exception {
		if (doSamples) {
			// Handle samples
			log.info("Handling samples");
			for (int i = 0; i < sampleAccs.size(); i += samplesFetchStep) {
				List<String> theseSampleAccs = sampleAccs.subList(i, Math.min(i + samplesFetchStep, sampleAccs.size()));
				// have to create multiple beans via context so they all have
				// their own dao object
				// this is apparently bad Inversion Of Control but I can't see a
				// better way to do it
				SampleRepoCallable callable = context.getBean(SampleRepoCallable.class, samplesClient, theseSampleAccs);

				if (poolThreadCount == 0) {
					callable.call();
				} else {
					futures.add(threadPool.submit(callable));
				}
			}
		}
	}

	/**
	 * Reads a list of named files or (stdin) and extracts sample and group accessions into the appropriate
	 * lists on this object.
	 * @param filenames
	 */
	private void handleFilenames(List<String> filenames) {
		for (String filename : filenames) {
			//read from standard in if a filename is --
			if (filename.equals("--")) {
				log.info("Reading accessions from standard input");
				try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
					handleBufferedReader(br);
			    } catch (FileNotFoundException e) {
					log.error("Unable to find "+filename, e);
				} catch (IOException e) {
					log.error("Unable to read "+filename, e);
				}
			} else {
				log.info("Reading accessions from "+filename);
				File file = new File(filename);
				if (file.exists() && file.isFile()) {		
					try (BufferedReader br = new BufferedReader(new FileReader(file))) {
						handleBufferedReader(br);
				    } catch (FileNotFoundException e) {
						log.error("Unable to find "+filename, e);
					} catch (IOException e) {
						log.error("Unable to read "+filename, e);
					}
				}
			}
		}
		//slice down to section specified by arguments
		int count = sampleAccs.size();
		int offsetSize = count / offsetTotal;
		int start = offsetSize * offsetCount;
		sampleAccs = sampleAccs.subList(start, start+offsetSize);

		count = groupAccs.size();
		offsetSize = count / offsetTotal;
		start = offsetSize * offsetCount;
		groupAccs = groupAccs.subList(start, start+offsetSize);
	}

	/**
	 * Reads from a buffered reader and extracts sample and group accessions into the appropriate
	 * lists on this object.
	 */
	private void handleBufferedReader(BufferedReader br) throws IOException {
		String line;
		while ((line = br.readLine()) != null) {
			line = line.trim();
			log.debug("reading line '"+line+"'");
	        if (line.matches("^SAM[END]A?[0-9]+$")) {
	        	if (!sampleAccs.contains(line)) {
	        		log.debug("adding sample accession "+line);
	        		sampleAccs.add(line);
	        	}
	        } else if(line.matches("^SAM[END]G[0-9]+$")) {
				if (!groupAccs.contains(line)) {
	        		log.debug("adding group accession "+line);
	        		groupAccs.add(line);
	        	}
	        }  
		}
	}
		
}
